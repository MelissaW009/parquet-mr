/**
 * Copyright 2012 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package parquet.column.values.deltastrings;

import java.io.IOException;

import parquet.column.values.ValuesReader;
import parquet.column.values.delta.DeltaBinaryPackingValuesReader;
import parquet.column.values.deltalengthbytearray.DeltaLengthByteArrayValuesReader;
import parquet.io.api.Binary;

/**
 * 
 * @author amokashi
 *
 */
public class DeltaStringValuesReader extends ValuesReader {
  private ValuesReader prefixLengthReader;
  private ValuesReader suffixReader;

  private byte[] previous = new byte[0];

  public DeltaStringValuesReader() {
    this.prefixLengthReader = new DeltaBinaryPackingValuesReader();
    this.suffixReader = new DeltaLengthByteArrayValuesReader();
    this.previous = new byte[0];
  }

  @Override
  public void initFromPage(int valueCount, byte[] page, int offset)
      throws IOException {
    prefixLengthReader.initFromPage(valueCount, page, offset);
    int next = prefixLengthReader.getNextOffset();
    suffixReader.initFromPage(valueCount, page, next);	
  }

  @Override
  public void skip() {
    prefixLengthReader.skip();
    suffixReader.skip();
  }

  @Override
  public Binary readBytes() {
    int prefixLength = prefixLengthReader.readInteger();
    // This does not copy bytes
    Binary suffix = suffixReader.readBytes();
    int length = prefixLength + suffix.length();
    
    // We have to do this to materialize the output
    byte[] out = new byte[length];
    System.arraycopy(previous, 0, out, 0, prefixLength);
    System.arraycopy(suffix.getBytes(), 0, out, prefixLength, suffix.length());
    
    previous = out;
    return Binary.fromByteArray(out);
  }
}