import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;


public class MMFileNamesManager {
	private String dirName;
	private ArrayList<String> channelsList;

	public MMFileNamesManager(String dirName) {
		this.dirName = dirName;
		this.channelsList=null;
	}

	/*
	 * Returns all the channels of the movies existing in the relevant dir.
	 */
	public ArrayList<String> getDirChannels(){
		if(channelsList!=null){
			return channelsList;
		}		
		ArrayList<String> channels= new ArrayList<String>();
		File f = new File(dirName);
		ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(f.list()));
		for(int i=0; i<fileNames.size(); i++){
			PathTokens pt=new PathTokens(fileNames.get(i));
			if( !channels.contains(pt.getChannel()) && pt.getChannel()!=(null)){
				channels.add(pt.getChannel());
			}					
		}
		channelsList=channels;
		return channelsList;		
	}

	/*
	 * Returns all the offsets of the images existing in the relevant channels.
	 */
	public ArrayList<Integer> getChannelOffsets(String channel){
		ArrayList<Integer> offsets= new ArrayList<Integer>();
		File f = new File(dirName);
		ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(f.list()));
		for(int i=0; i<fileNames.size(); i++){
			PathTokens pt=new PathTokens(fileNames.get(i));
			if( !offsets.contains(pt.getOffset()) && pt.getChannel()!=(null) && pt.getChannel().compareTo(channel)==0){
				offsets.add(pt.getOffset());
			}					
		}		
		return offsets;		
	}

	public  ArrayList<String> getPathsOfFrame(int frame) {
		ArrayList<String> paths= new ArrayList<String>();
		String regExp="cmd /c dir /b "+dirName+"\\img_"+String.format("%09d", frame)+"_*.tif";
		boolean hasFiles=false; 
		int tries=0;
		while(!hasFiles && tries<2){
			Process p;
			tries++;
			try {
				p = Runtime.getRuntime().exec(regExp);
				try {
					p.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
				BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line=reader.readLine();
				while(line!=null)
				{
					paths.add(line);
					line=reader.readLine();
				}
			} catch (Exception e) {

				e.printStackTrace();
				return null;
			}
			if(paths.size()==0){
				regExp="cmd /c dir /b "+dirName+"\\img_"+frame+"_*.tif";				
			}
			else{
				hasFiles=true;
			}
		}
		return paths;
	}


	private void While(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public ArrayList<Integer> getOffsetsOfFrameChannel(int frame, String channel) throws IOException, InterruptedException{
		ArrayList<Integer> offsets= new ArrayList<Integer>();
		String regExp="dir /b img_"+String.format("%09d", frame)+"_"+channel+"_*.tif";
		Process p=Runtime.getRuntime().exec(regExp);
		p.waitFor();
		BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line=reader.readLine();
		while(line!=null)
		{
			PathTokens pt=new PathTokens(line);
			offsets.add(pt.getOffset());
		}
		return offsets;
	}

	public ArrayList<String> getChannelsOfFrame(int frame) throws IOException, InterruptedException{
		ArrayList<String> channels= new ArrayList<String>();
		String regExp="dir /b img_"+String.format("%09d", frame)+"_*.tif";
		Process p=Runtime.getRuntime().exec(regExp);
		p.waitFor();
		BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line=reader.readLine();
		while(line!=null)
		{
			PathTokens pt=new PathTokens(line);
			channels.add(pt.getChannel());
		}
		return channels;
	}



	/*
	 * Returns the filename of the file representing the image taken for the given channel, frame and offset
	 */
	public String getPath(String channel, int frame, int offset){
		return "img_"+String.format("%09d", frame)+"_"+channel+"_"+(String.format("%03d", offset));
	}	

	public String getDirName() {
		return dirName;
	}

	public void setDirName(String dirName) {
		this.dirName = dirName;
	}



}
