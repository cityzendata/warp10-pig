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
package io.warp10.pig;

import io.warp10.hadoop.Warp10InputFormat;

import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.pig.Expression;
import org.apache.pig.LoadFunc;
import org.apache.pig.LoadMetadata;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceStatistics;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.BinSedesTupleFactory;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;

public class Warp10LoadFunc extends LoadFunc implements LoadMetadata {

  private TupleFactory tfactory = new BinSedesTupleFactory();
  private RecordReader reader;
  private PigSplit split;
  private String location;
  private String udfcSignature;

  private String splitsEndpoint;
  private String splitsSelector;
  private String splitsToken;

  static {
    PigWarpConfig.ensureConfig();
  }
  
  private String suffix = null;
  
  public Warp10LoadFunc() {
    this.suffix = null;
  }
  
  public Warp10LoadFunc(String suffix) {
    this.suffix = suffix;
  }
  
  @Override
  public InputFormat getInputFormat() throws IOException {
    return (InputFormat) new Warp10InputFormat(this.suffix);
  }

  @Override
  public Tuple getNext() throws IOException {

    try {
      if (!this.reader.nextKeyValue()) {
        return null;
      }

      String key = this.reader.getCurrentKey().toString();
      BytesWritable value = (BytesWritable) this.reader.getCurrentValue();

      Tuple t = this.tfactory.newTuple(2);

      t.set(0, key);
      t.set(1, new DataByteArray(value.copyBytes()));

      return t;
    } catch (InterruptedException ie) {
      throw new IOException(ie);
    }
  }

  @Override
  public void prepareToRead(RecordReader reader, PigSplit split) throws IOException {
    this.reader = reader;
    this.split = split;
  }

  @Override
  public void setLocation(String location, Job job) throws IOException {
    this.location = location;
  }

  @Override
  public ResourceSchema getSchema(String location, Job job) throws IOException {
    ResourceSchema schema = new ResourceSchema();

    ResourceSchema.ResourceFieldSchema[] fields = new ResourceSchema.ResourceFieldSchema[2];

    fields[0] = new ResourceSchema.ResourceFieldSchema(new Schema.FieldSchema("id", DataType.CHARARRAY));
    fields[1] = new ResourceSchema.ResourceFieldSchema(new Schema.FieldSchema("data", DataType.BYTEARRAY));

    schema.setFields(fields);
    return schema;
  }

  @Override
  public void setUDFContextSignature(String signature) {
    this.udfcSignature = signature;
  }

  @Override
  public String[] getPartitionKeys(String location, Job job) throws IOException {
    return null;
  }

  @Override
  public ResourceStatistics getStatistics(String location, Job job) throws IOException {
    return null;
  }

  @Override
  public void setPartitionFilter(Expression partitionFilter) throws IOException {
  }

}
