//
//   Copyright 2018  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package io.warp10.pig.utils;

import java.io.IOException;
import java.util.List;

import io.warp10.continuum.store.thrift.data.GTSWrapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;

import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.pig.StoreFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;

public class SequenceFileWriter extends StoreFunc {

  private String storeLocation;
  private RecordWriter writer;
  private Job job;

  private Class<? extends WritableComparable> keyClass;
  private Class<? extends Writable> valueClass;


  @SuppressWarnings({ "unchecked", "rawtypes" })
  public SequenceFileWriter(String... args)
      throws IOException {
    try {
      this.keyClass = NullWritable.class;
      this.valueClass = (Class<? extends Writable>) Class.forName(args[0]);
    }
    catch (Exception e) {
      throw new IOException("Invalid key/value type", e);
    }
  }


  @Override
  public OutputFormat getOutputFormat() throws IOException {
    return new SequenceFileOutputFormat()
    {
      public RecordWriter getRecordWriter( TaskAttemptContext context )
          throws IOException, InterruptedException
      {
        Configuration conf = context.getConfiguration();

        CompressionCodec codec = new DefaultCodec();
        SequenceFile.CompressionType compressionType = SequenceFile.CompressionType.BLOCK;

        // get the path of the temporary output file
        Path file = getDefaultWorkFile(context, "");

        final SequenceFile.Writer out = SequenceFile.createWriter(conf,
            SequenceFile.Writer.compression(SequenceFile.CompressionType.BLOCK, new DefaultCodec()),
            SequenceFile.Writer.keyClass(NullWritable.class),
            SequenceFile.Writer.valueClass(BytesWritable.class),
            SequenceFile.Writer.file(file));

        return new RecordWriter() {

          public void write( Object key, Object value)
              throws IOException {
            out.append(key, value);
          }

          public void close(TaskAttemptContext context) throws IOException {
            out.close();
          }
        };
      }
    };
  }

  @Override
  public void setStoreLocation(String location, Job job) throws IOException {
    this.storeLocation = location;
    this.job = job;
    this.job.setOutputKeyClass(keyClass);
    this.job.setOutputValueClass(valueClass);
    FileOutputFormat.setOutputPath(job, new Path(location));
    FileOutputFormat.setCompressOutput(this.job, true);
    FileOutputFormat.setOutputCompressorClass(this.job, org.apache.hadoop.io.compress.DefaultCodec.class);
  }

  @Override
  public void prepareToWrite(RecordWriter writer) throws IOException {
    this.writer = writer;
  }

  /**
   *
   * @param input : tuple with one field (encoded : GTSWrapper)
   * @throws IOException
   */
  @Override
  public void putNext(Tuple input) throws IOException {
    try {

      if (input.size() != 1) {
        throw new IOException("Invalid input, expecting Tuple: (gtswrapper)");
      }

      if (!(input.get(0) instanceof DataByteArray)) {
        throw new IOException("Invalid input, expecting Tuple: (gtswrapper)");
      }

      DataByteArray data = (DataByteArray) input.get(0);

      writer.write(NullWritable.get(), new BytesWritable(data.get()));

    } catch (Exception e) {
      throw new IOException(e);
    }

  }
}