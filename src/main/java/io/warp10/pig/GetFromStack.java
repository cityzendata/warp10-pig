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

import io.warp10.pig.utils.WarpScriptUtils;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import java.io.IOException;
import java.util.Iterator;

/**
 * Get object from stack at one position
 */
public class GetFromStack extends EvalFunc<Tuple> {

  public GetFromStack() { }

  /**
   *
   * @param input : a Tuple with the search index and the stack => (index: int, stack: {})
   * @return Object at this position in the stack
   * @throws IOException
   */
  @Override
  public Tuple exec(Tuple input) throws IOException {

    if (2 != input.size()) {
      throw new IOException(
          "Invalid input, expecting a tuple with the stack level and the current stack - (index, stack:{})");
    }

    reporter.progress();

    //
    // Get the search index (first field)
    //

    int index = (int) input.get(0);

    DataBag stack = (DataBag) input.get(1);

    Iterator<Tuple> iter = stack.iterator();

    //
    // FIXME : try to find the element at the given index more efficiently (Warning : we manipulate a Bag)
    //

    while (iter.hasNext()) {

      Tuple tuple = iter.next();

      if (2 != tuple.size()) {
        throw new IOException("Invalid input, expecting a tuple with the stack level and the current stack - (index, stack:{})");
      }

      int level = (int) tuple.get(0);

      if (index == level) {

        //
        // Element found - return this tuple
        //

        return tuple;

      }

    }

    return null;

  }

  @Override
  public Schema outputSchema(Schema input) {
    return null;
  }

}
