// Copyright 2022 Proyectos y Sistemas de Mantenimiento SL (eProsima).
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.eprosima.fastdds.idl.parser.typecode;

import com.eprosima.idl.parser.exception.RuntimeGenerationException;

public interface TypeCode
{
    static long cdr_alignment(
            long current_alignment,
            long data_size)
    {
        return (data_size - (current_alignment % data_size)) & (data_size - 1);
    }

    /*
     * Returns the maximum serialized size between XCDRv1 and XCDRv2.
     */
    public long maxSerializedSize(
            long current_alignment);

    /*
     * Returns the maximum serialized size for a plain Type
     */
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64) throws RuntimeGenerationException;
}
