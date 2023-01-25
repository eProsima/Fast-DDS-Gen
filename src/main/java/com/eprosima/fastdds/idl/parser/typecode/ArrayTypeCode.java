// Copyright 2021 Proyectos y Sistemas de Mantenimiento SL (eProsima).
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

public class ArrayTypeCode extends com.eprosima.idl.parser.typecode.ArrayTypeCode
    implements TypeCode
{

    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;
        long size = 1;
        for (int count = 0; count < getDimensions().size(); ++count)
        {
            size += Long.parseLong(getDimensions().get(count), 10);
        }

        for (long count = 0; count < size; ++count)
        {
            current_alignment += ((TypeCode)getContentTypeCode()).maxSerializedSize(current_alignment);
        }

        return current_alignment - initial_alignment;
    }

}
