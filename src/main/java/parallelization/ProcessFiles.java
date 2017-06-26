package parallelization;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import interactiveMT.BatchMode;
import interactiveMT.Interactive_MTDoubleChannel;

public class ProcessFiles  {

	public static void process(File[] directory, ExecutorService taskexecutor) {

		for (int fileindex = 0; fileindex < directory.length; ++fileindex) {
		
		
			BatchMode parent = new BatchMode(directory, new Interactive_MTDoubleChannel(), directory[0]);
			
			taskexecutor.execute(new Split(directory[fileindex], parent, fileindex));
			
		}
		
		taskexecutor.shutdown();
		
		try {
			taskexecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		}

	

}