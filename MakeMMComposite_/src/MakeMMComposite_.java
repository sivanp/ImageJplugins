
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ShortProcessor;
// Java 1.1
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.plaf.ListUI;

import org.w3c.dom.css.RGBColor;
 

public class MakeMMComposite_  implements PlugIn {
	/* begin class StackReg_ */

	/*....................................................................
		Private global variables
	....................................................................*/

	String inDirName;
	String[] RGBchannels=new String[4];
	String[] messages={"Choose red channel","Choose green channel","Choose blue channel","Choose gray channel"};
	String[] defaults={"KO","GFP","*None*","Phase"};
	int[] RGBoffsets=new int[4]; 
	//String[] channelNames={"KO","GFP","Blue","Phase"};
	ArrayList<Integer> workingSet=new ArrayList<Integer>();
	MMFileNamesManager MMM;
	/*....................................................................
		Public methods
	....................................................................*/

	/********************************************************************/
	public void run (final String arg) {
		Runtime.getRuntime().gc();

		DirectoryChooser od1 = new DirectoryChooser("SELECT INPUT DIRECTORY");
		inDirName = od1.getDirectory(); 

		 MMM=new MMFileNamesManager(inDirName);
		ArrayList<String> channels= MMM.getDirChannels(); 
		channels.add("*None*");
		final String[] channelItem = channels.toArray(new String[channels.size()]);

		for (int i=0; i<RGBchannels.length; i++){
			GenericDialog gd = new GenericDialog(messages[i]);		
			gd.addChoice("Channel To align by:", channelItem, defaults[i]);
			gd.showDialog();		
			if (gd.wasCanceled()) {
				return;
			}
			int channelNum = gd.getNextChoiceIndex();
			RGBchannels[i]=channelItem[channelNum]; 

			gd = new GenericDialog("Choose Offset");		
			ArrayList<Integer> offsets= MMM.getChannelOffsets(RGBchannels[i]);
			ArrayList<String> offNames=new ArrayList<String>();
			for(int j=0;j<offsets.size(); j++){
				offNames.add(offsets.get(j).toString());
			}	

			if(offNames.size()>0){
				String[] offsetItem= offNames.toArray(new String[offNames.size()]);
				gd.addChoice("Channel's offset To align by:", offsetItem, "000");

				gd.showDialog();
				if (gd.wasCanceled()) {
					return;
				}
				int offsetNum = gd.getNextChoiceIndex();
				RGBoffsets[i]=Integer.parseInt(offsetItem[offsetNum]); 
			}
		}
		
		for (int i=0; i<RGBchannels.length;i++){
			if(RGBchannels[i].equals("*None*")){
				continue;
			}	
			workingSet.add(i);
			IJ.run("Image Sequence...", "open="+inDirName+"\\ file="+RGBchannels[i]+"_"+String.format("%03d", RGBoffsets[i]) + " or=[] sort use");
			ImagePlus imp = WindowManager.getCurrentImage();
			imp.setTitle(RGBchannels[i]);

		} 
		//just to make sure there's something to work with....
		final ImagePlus imp2 = WindowManager.getCurrentImage();
		if (imp2 == null) {
			IJ.error("No images available");
			return;
		}	
		removeNonIntersectFrames();
		IJ.run(imp2, "Merge Channels...", "red="+RGBchannels[0]+" green="+RGBchannels[1]+" blue="+RGBchannels[2]+" gray="+RGBchannels[3]+" create");
	} 
	

	private void removeNonIntersectFrames(){
		ArrayList<Integer> framesIntersection=new ArrayList<Integer>();
		//first calculate the set of all working frames
		ArrayList<Integer> frames=new ArrayList<Integer>();
		ImagePlus imp=WindowManager.getImage(RGBchannels[workingSet.get(0)]);		
		int s=0;		
		while (s<imp.getNSlices()){
			s++;
			imp.setSlice(s);			
			frames.add(PathTokens.getCurFrame(imp));			
		} 
		for (int i=1; i<workingSet.size();i++){
			imp=WindowManager.getImage(RGBchannels[workingSet.get(i)]);
			s=0;	
			ArrayList<Integer> frames2=new ArrayList<Integer>();
			while (s<imp.getNSlices()){
				s++;
				imp.setSlice(s);
				frames2.add(PathTokens.getCurFrame(imp));		
			} 
			
			
			framesIntersection=new ArrayList<Integer>();
			for(int j=0;j<frames.size();j++){
				if(frames2.contains(frames.get(j))){
					framesIntersection.add(frames.get(j));
				}
			}
			frames=framesIntersection;
		}
		//now we have the complete intersection- start removing frames which are not there
		for (int i=0; i<workingSet.size();i++){
			imp=WindowManager.getImage(RGBchannels[workingSet.get(i)]);
			s=0;			
			while (s<imp.getNSlices()){
				s++;
				imp.setSlice(s);
				if(!framesIntersection.contains(PathTokens.getCurFrame(imp))){
					IJ.run(imp, "Delete Slice", "");
					s--;
				}
			} 
		}
		

	}
}

