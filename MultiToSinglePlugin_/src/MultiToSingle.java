
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.io.OpenDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.lang.Exception;

public class MultiToSingles_Plugin_ implements PlugIn {

	public void run(String arg)  {
		int skip=1;
		int  fucciskip=3;
		int fileNum=1;
		String mdir="C:\\Users\\sivan-nqb\\Desktop\\1july13 microfluidics\\test";
		File directory = new File(mdir);
		File[] mfiles = directory.listFiles();
    		fileNum=mfiles.length;
		Arrays.sort(mfiles);
		GenericDialog gd= new GenericDialog("bla");
    		gd.addMessage("mfiles: "+fileNum);
    		gd.showDialog();				
		
    		for (int i=0; i<fileNum; i++){
    			String fname=mfiles[i].getAbsolutePath();
    			 gd= new GenericDialog("bla");
    		gd.addMessage("file name: "+fname);
    			ImagePlus imp = IJ.openImage(fname);
    			int slicesNum=imp.getStackSize();
    			for(int j=0; j<slicesNum; j++){
    				imp.setSlice(j);
    				IJ.run("Copy"); 
    				ImagePlus newimp = IJ.createImage("8", "16-bit Black", 1024, 1024, 1);
    				IJ.run("Paste");
    				String newPath=i+"_"+j;
    				IJ.saveAs(newimp,"Tiff",newPath);
    				newimp.close();
    			}    			
    		}
	}
}
