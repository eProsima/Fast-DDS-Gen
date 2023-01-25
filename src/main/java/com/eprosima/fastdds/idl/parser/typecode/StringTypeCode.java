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

import com.eprosima.idl.parser.typecode.Kind;

public class StringTypeCode extends com.eprosima.idl.parser.typecode.StringTypeCode
    implements TypeCode
{
    public StringTypeCode(
            int kind,
            String maxsize)
    {
        super(kind, maxsize);
    }

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;
        long maxsize = Long.parseLong(getMaxsize(), 10);

        switch (getKind())
        {
            case Kind.KIND_STRING:
                current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4) + (maxsize * 4);
                break;
            case Kind.KIND_WSTRING:
                current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4) + maxsize + 1;
                break;
        }

        return current_alignment - initial_alignment;
    }

}
