import ij.*;
import ij.plugin.*;

public class LoadImages_ implements PlugIn {

	public void run(String arg) {
		IJ.run("Image Sequence...", "open=C:\\Users\\sivan-nqb\\Desktop\\samples\\easy8bit\\L1210wt_ph_expo40_1_1002.tif number=3 starting=1 increment=1 scale=100 file=[] or=[] sort");
	}

}
