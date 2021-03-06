import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Thresholder;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;


public class OfTrack_ extends  MTrack3_ 
{ 

	Cells cellsStruct;
	Map<Integer,Integer> mapFrameSlice;


	@Override
	public int setup(String arg, ImagePlus imp) 
	{
		return super.setup(arg,imp);	
	}

	@Override
	public void run(ImageProcessor ip) 
	{
		Vector<Vector<Particle>> theTracks= track(imp, 10, 800,(float) 100.0,null ,null) ;
		 IJ.setAutoThreshold(imp, "Default");		
		cellsStruct = new Cells();

		mapFrameSlice = getFrameSliceMap();

		ResultsTable positionTable = new ResultsTable();
		
		positionTable.reset();
		
		int theTrackssize =  theTracks.size();
		
		for(int i = 0; i < theTrackssize; i++)
		{

			IJ.showProgress((double)i /theTrackssize );
			
			Vector<Particle> trackParticles = theTracks.get(i);
			if(trackParticles.size() >= minTrackLength)
			{

				Cell childCell = cellsStruct.addNewCell();
				for(Particle aParticle : trackParticles)
				{
					int slice= aParticle.z; 

					PathTokens pt=new PathTokens(imp.getStack(),slice);
					imp.setSlice(slice);

					IJ.doWand((int)aParticle.x, (int)aParticle.y);
					Roi roi = imp.getRoi();
					childCell.addLocation(pt.getFrame(), roi);

					positionTable.incrementCounter();
					aParticle.displayTrackNr = i+1;
					positionTable.addValue("CellID",childCell.id);
					positionTable.addValue("Track", aParticle.displayTrackNr);
					positionTable.addValue("Frame", aParticle.z);
					positionTable.addValue("X", aParticle.x);
					positionTable.addValue("velX", aParticle.velX);
					positionTable.addValue("Y", aParticle.y);
					positionTable.addValue("velY", aParticle.velY);
					positionTable.addValue("Flags", (aParticle.flag?1:0));
					
				}
			}
		}
		positionTable.show("Particle positions");
		IJ.showStatus("starting to look for mommies");
		imp.setSlice(1);
		int firstFrame=PathTokens.getCurFrame(imp);
		
		// find Cells starting not in the first frame.			
		Set<Integer> cellsSet=cellsStruct.keySet();
		
		SortedSet<Cell> cellsSortedByFirstFrame = new TreeSet<Cell>(new CellFramesComparator());
		cellsSortedByFirstFrame.addAll(cellsStruct.values());
	
		
		int[] cellIds = new int[cellsSortedByFirstFrame.size()];
		int i=0;
		
		
		for (Cell cell:cellsSortedByFirstFrame )
		{
			//IJ.showStatus("cell id: "+cellIds[i]);
			cellIds[i] = cell.getId();
			i++;
		}
		
		
		int cellIdsSize = cellIds.length;
		for(int j=0; j<cellIdsSize;j++)
		{
			IJ.showProgress((double)j /cellIdsSize );
			Cell cell=cellsStruct.get(cellIds[j]);
			if ((!cell.getFrames().contains(firstFrame) )&& cell.getMothers()==null )
			{//not in first frame and No mommy Looking for my mommy
				updateMyMother(cell);

			}

		}


		SaveDialog sd = new SaveDialog("Save cell structure", "cell Struct", "");
		
		  
		    String dir=sd.getDirectory();
		    String name=sd.getFileName();
		    if(name==null)
		        return;
		   String path = dir + name;
		    
		    saveCellsStruct(path);

	}



	private Map<Integer,Integer> getFrameSliceMap()
	{
		Map<Integer,Integer> mapFrameSlice = new HashMap<Integer, Integer>();
		for (int slice=1 ;slice<= imp.getStackSize();slice++)
		{
			imp.setSlice(slice);
			PathTokens pt=new PathTokens(imp.getStack(),slice);
			mapFrameSlice.put(pt.getFrame(), slice) ;
		}

		return mapFrameSlice;	
	}

	private void updateMyMother(Cell cell) 
	{
		System.out.println(cell.getId());
		Integer[] cellFrames =  cell.getFrames().toArray(new Integer[0]) ;
		Arrays.sort(cellFrames);
		int firstCellSlice = mapFrameSlice.get(cellFrames[0]) ;

		// look for Mother at firstCellSlice - 1 
		PathTokens pt=new PathTokens(imp.getStack(),firstCellSlice-1);
		int motherLastFrame =  pt.getFrame();

		//find the closest mom in town. 
		double Score = Double.MAX_VALUE;
		Cell mother=null;
		for (Cell mightBMomy : cellsStruct.getCellsInFrame(motherLastFrame))
		{
			double newScore =  getCellsScore(cell,cellFrames[0], mightBMomy,motherLastFrame);
			if (newScore<Score)
			{
				Score  = newScore;
				mother = mightBMomy;
			}
		}

		if (mother!=null)
		{

			//TODO:Max Distance
			//TODO:Add Size consideration
			// Create new cell for sister  and move frames from mother sell
			Cell sister = cellsStruct.addNewCell();
			Integer[] motherFrames =  mother.getFrames().toArray(new Integer[0]);
			Arrays.sort(motherFrames);
			int motherLastFrameind = Arrays.binarySearch(motherFrames, motherLastFrame);
			for (int i = motherLastFrameind+1 ; i<motherFrames.length;i++)
			{
				int frame =motherFrames[i] ;
				sister.addLocation(frame, mother.getLocationInFrame(frame));			
				mother.deleteLocation(frame);
			}
			cell.addMother(mother);
			sister.addMother(mother);
		}
	}
	private double getCellSize(Cell cell,int frame)
	{
		PolygonRoi roi = cell.getLocationInFrame(frame).getRoi();
		imp.setRoi(roi);
		ImageStatistics stats= ImageStatistics.getStatistics(imp.getProcessor(), Measurements.MEAN,imp.getCalibration());
		double area = stats.area;
		return area;
	}


	private double getCellsScore(Cell childCell,int childFrame,Cell momCell,int momFrame)
	{
		double cellsScore = Double.MAX_VALUE;
		//TODO:create Parameters
		if (getCellsDistance( childCell, childFrame, momCell, momFrame) < 30 && momCell.getLocationInFrame(childFrame)!=null)
		{			
			cellsScore = Math.abs(1- ( getCellSize(momCell,momFrame) /(getCellSize(childCell,childFrame) + getCellSize(momCell,childFrame) )));
			
		}
		if (cellsScore>0.4)
		{
			return Double.MAX_VALUE;
		}
		else {
			return getCellsDistance( childCell, childFrame, momCell, momFrame);
		}
	}


	private double getCellsDistance(Cell childCell,int childFrame,Cell momCell,int momFrame)
	{ 
		double X1 = childCell.getLocationInFrame(childFrame).getRoi().getBounds().getCenterX();
		double X2 = momCell.getLocationInFrame(momFrame).getRoi().getBounds().getCenterX();
		double Y1 = childCell.getLocationInFrame(childFrame).getRoi().getBounds().getCenterY();
		double Y2 = momCell.getLocationInFrame(momFrame).getRoi().getBounds().getCenterY();

		double Distance =Math.sqrt( Math.pow(X1-X2, 2) +  Math.pow(Y1-Y2, 2));
		return Distance;


	}


	protected void saveCellsStruct(String filename){		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try{
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);
			out.writeObject(cellsStruct);		
			out.close();
		}
		catch(IOException ex)
		{
			IJ.showMessage("Save cellsStruct failed ","io: "+ex);
			ex.printStackTrace();
		}

	}

	
	private class CellFramesComparator implements Comparator<Cell>
	{

		@Override
		public int compare(Cell c1, Cell c2) {
			
			return c1.getFrames().first() -  c2.getFrames().first();
		}
		
	}


}
