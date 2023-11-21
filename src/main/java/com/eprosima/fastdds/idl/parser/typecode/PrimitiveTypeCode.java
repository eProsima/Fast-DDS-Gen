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

public class PrimitiveTypeCode extends com.eprosima.idl.parser.typecode.PrimitiveTypeCode
    implements TypeCode
{

    public PrimitiveTypeCode(
            int kind)
    {
        super(kind);
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

        switch (getKind())
        {
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONGDOUBLE:
                current_alignment += 16 + TypeCode.cdr_alignment(current_alignment, align64);
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_DOUBLE:
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONGLONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_ULONGLONG:
                current_alignment += 8 + TypeCode.cdr_alignment(current_alignment, align64);
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_ULONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_FLOAT:
                current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4);
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_SHORT:
            case com.eprosima.idl.parser.typecode.Kind.KIND_USHORT:
            case com.eprosima.idl.parser.typecode.Kind.KIND_WCHAR:
                current_alignment += 2 + TypeCode.cdr_alignment(current_alignment, 2);
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_BOOLEAN:
            case com.eprosima.idl.parser.typecode.Kind.KIND_CHAR:
            case com.eprosima.idl.parser.typecode.Kind.KIND_OCTET:
            case com.eprosima.idl.parser.typecode.Kind.KIND_INT8:
            case com.eprosima.idl.parser.typecode.Kind.KIND_UINT8:
                current_alignment += 1;
                break;
        }

        return current_alignment - initial_alignment;
    }
}
