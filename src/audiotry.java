import org.python.util.PythonInterpreter;

import java.util.Properties;

import org.python.core.Py;
import org.python.core.PySystemState;

public class audiotry {

	public static void main(String[] args) {
		PySystemState sys = Py.getSystemState();
		System.out.println(sys.path.toString());
		Properties preprops = System.getProperties();
		Properties props = new Properties(); 
		props.put("python.home", "C:\\Program Files\\Anaconda3\\Lib");
		String[] s = {};
		PythonInterpreter.initialize(preprops, props, s);
		PythonInterpreter interpreter = new PythonInterpreter();
		interpreter.execfile("D:\\Codes\\pythonForJava\\audioTranslate.py");
		
	}
	
}
