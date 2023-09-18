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

public class BitmaskTypeCode extends com.eprosima.idl.parser.typecode.BitmaskTypeCode
    implements TypeCode
{
    public BitmaskTypeCode(
            String scope,
            String name)
    {
        super(scope, name);
    }

    public BitmaskTypeCode(
            String scope,
            String name,
            Integer bit_bound)
    {
        super(scope, name, bit_bound);
    }

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        return maxPlainTypeSerializedSize(current_alignment, 8);
    }

    @Override
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64)
    {
        long initial_alignment = current_alignment;
        long size = Long.parseLong(getSize(), 10);

        current_alignment += size + TypeCode.cdr_alignment(current_alignment, 4 < size ? align64 : size);

        return current_alignment - initial_alignment;
    }
}
