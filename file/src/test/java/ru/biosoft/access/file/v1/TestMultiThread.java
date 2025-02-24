package ru.biosoft.access.file.v1;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;

public class TestMultiThread {
	public static void main(String[] args) throws Exception {
		Environment.INSTANCE = new TestEnv();
		Environment env = TestEnv.INSTANCE;
		
		Properties properties = new Properties();
		properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "test");
		properties.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, "test");
		FileDataCollectionV1 fdc = new FileDataCollectionV1(null, properties );

		ScheduledExecutorService pool = Executors.newScheduledThreadPool(10);
		
		Random rnd = new Random();
		for(int i = 0; i < 10; i++)
		{
			final int I = i;
			pool.scheduleWithFixedDelay(() -> {
				File file;
				try {
					file = Files.createTempFile("de", ".txt").toFile();
					
					DataElement de = env.createFileDataElement("de" + I + "_" + rnd.nextInt(20), fdc, file);
					fdc.put(de);
					file.delete();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				
			}, 0, 100, TimeUnit.MILLISECONDS);
		}
		
		while(true)
		{
			List<String> nameList = fdc.getNameList();
			System.out.println(nameList);
			for(String name : nameList)
			{
				fdc.remove(name);
			}
			Thread.sleep(1000);
		}
		
	}
}
