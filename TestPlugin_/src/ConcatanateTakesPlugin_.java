
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.OpenDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.lang.Exception;
import ij.io.DirectoryChooser;

public class ConcatanateTakesPlugin_ implements PlugIn {

	public void run(String arg)  {
		int skip=1;		
		
		int dirNum=1;
		DirectoryChooser dc= new DirectoryChooser("choose directory");
		String mdir= dc.getDirectory();
		//OpenDialog op=new OpenDialog("choose the directory with all takes","");
		//String mdir=op.getDirectory();
		//String mdir="C:\\Users\\sivan-nqb\\Desktop\\1july13 microfluidics";
		FilenameFilter dirsFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {            			
				return new File(dir, name).isDirectory();            			
			}
		};
		File directory = new File(mdir);
		File[] mdirs = directory.listFiles(dirsFilter);
		dirNum=mdirs.length;
		Arrays.sort(mdirs);
		GenericDialog gd= new GenericDialog("bla");
		gd.addMessage("mdirs: "+mdirs.length);
		gd.showDialog();		
		int locNum=-1;
		for (int i=0; i<dirNum;i++){
			directory = mdirs[i];
			File[] locdirs = directory.listFiles(dirsFilter);
			if(locNum==-1){
				locNum=locdirs.length;

			}
			else if(locNum!=locdirs.length){
				//throw(new Exception("not same number od subdirectories"));  
				gd= new GenericDialog("bla");
				gd.addMessage("not same number of subdirectories");
				gd.showDialog();
				return;
			}
		}

		//now we start our work!
		String[] types ={"KO","GFP","Phase"};
		int[] skips = {3,3,1};
		
		for (int k=0; k<types.length; k++){
			for (int i=0;i<locNum;i++){
				String posName="";
				ImagePlus[] imps= new ImagePlus[dirNum];
				for(int j=0; j<dirNum; j++){
					directory = mdirs[j];
					File[] locdirs = directory.listFiles(dirsFilter);
					String dirName=locdirs[i].getAbsolutePath();
					posName=locdirs[i].getName();  
					IJ.run("Image Sequence...", "open=["+dirName+"\\img_000000000_Phase_000.tif] number=2191 starting=1 increment="+skip+" scale=100 file="+types[k]+" or=[] sort use");
					ImagePlus imp = IJ.getImage();
					imp.setTitle(types[k]+j);
					if(dirNum==1){
						imp.setTitle(types[k]);				
					}
					imps[j]=imp;

				}
				IJ.showStatus("working on location: "+i);
				if(dirNum>1){//do merges				
				/*	String[] merges=new String[dirNum];
							merges[0]="stack1="+types[k]+0;
					for(int l=1; l<dirNum; l++){
						merges[l]=merges+" stack2="+types[k]+l;
					}*/
					Concatenator conc= new Concatenator();
					ImagePlus imp=conc.concatenate(imps[0],imps[1],false);
					//IJ.run("Concatenate...", merges+" title="+types[k]);
					
					//saving the stack:
					//ImagePlus imp= ij.WindowManager.getCurrentImage();
					
					
					for(int l=2; l<imps.length; l++){						
						imp=conc.concatenate(imp,imps[l],false);
						
					}
					imp.show();
					int slicesNum=imp.getStackSize();    		
					for(int j=0; j<slicesNum; j++){   
						ImagePlus newimp = new Duplicator().run(imp,j+1,j+1);
						newimp.show();
						String newDirName=mdir+"\\"+posName;
						if(k==0){ //create the directory
							File dir = new File(newDirName);    			
							dir.mkdir();
						}
						String newPath=newDirName+"\\img_"+j*skips[k]+"_"+types[k]+"_000";
						IJ.saveAs(newimp,"Tiff",newPath);
						newimp.close();
					}
					
				}		

				
				ImagePlus imp = IJ.getImage();
				while (null != WindowManager.getCurrentImage()) {
					imp = WindowManager.getCurrentImage();
					imp.changes = false;
					imp.close();
				}          						
			}	
		}
	}
}

