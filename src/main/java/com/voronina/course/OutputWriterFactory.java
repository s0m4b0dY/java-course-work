package com.voronina.course;

public class OutputWriterFactory {
    public static OutputWriter create(OutputFileFormat format) {
        if (format == OutputFileFormat.CSV) {
            return new CsvOutputWriter();
        }

        return new JsonOutputWriter();
    }
}
