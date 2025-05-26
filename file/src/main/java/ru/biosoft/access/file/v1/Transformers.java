package ru.biosoft.access.file.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;

public class Transformers {
	
	private static Map<String, List<Transformer>> byExtension = new LinkedHashMap<>();
	private static Map<Class<? extends DataElement>, List<Transformer>> byOutputType = new LinkedHashMap<>();

	public static void registerTransformer(Transformer t, String fileExtension)
	{
		byExtension.computeIfAbsent(fileExtension, x->new ArrayList<>()).add(t);
		byOutputType.computeIfAbsent(t.getOutputType(), x->new ArrayList<>()).add(t);
	}
	
	public static List<Transformer> getByExtension(String ext)
	{
		return byExtension.get(ext);
	}
	
	public static List<Transformer> getByOutType(Class<? extends DataElement> outType)
	{
		return byOutputType.get(outType);
	}
	
	
	/*
	 switch(ext)
     {
         case "fna": case "fa": case "fasta": case "ffn": case "fsa": return new FastaFileTransformer();
         case "gbk": return new GenbankFileTransformer();
         case "gff": return new GFFFileTransformer();
         case "vcf": return new VCFFileTransformer();
         case "txt": case "tsv": case "tbl": case "log": return new FileTextTransformer();
         case "html": return new FileHtmlTransformer();
     }
	 */
}
