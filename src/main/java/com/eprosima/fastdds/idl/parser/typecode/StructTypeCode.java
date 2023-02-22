// Copyright 2016 Proyectos y Sistemas de Mantenimiento SL (eProsima).
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

import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.tree.Annotation;

import java.util.ArrayList;

public class StructTypeCode extends com.eprosima.idl.parser.typecode.StructTypeCode
    implements TypeCode
{
    public StructTypeCode(
            String scope,
            String name)
    {
        super(scope, name);
    }

    public boolean isHasKey()
    {
        boolean returnedValue = false;

        for (int count = 0; count < getMembers().size() && !returnedValue; ++count)
        {
            Member member = getMembers().get(count);
            returnedValue = member.isAnnotationKey();
        }

        return returnedValue;
    }

    public String getMaxSerializedSize()
    {
        return Long.toString(maxSerializedSize(0));
    }

    public long maxSerializedSize(
            long current_alignment)
    {
        return maxSerializedSize(current_alignment, false);
    }

    private long maxSerializedSize(
            long current_alignment,
            boolean only_keys)
    {
        long initial_alignment = current_alignment;

        for (com.eprosima.idl.parser.typecode.TypeCode parent : getInheritances())
        {
            current_alignment += ((StructTypeCode)parent).maxSerializedSize(current_alignment, only_keys);
        }

        for (Member member : getMembers())
        {
            if (member.isAnnotationNonSerialized())
            {
                continue;
            }
            if (only_keys && isHasKey())
            {
                if (member.isAnnotationKey())
                {
                    if (member.getTypecode() instanceof StructTypeCode && ((StructTypeCode)member.getTypecode()).isHasKey())
                    {
                        current_alignment +=
                            ((StructTypeCode)member.getTypecode()).maxSerializedSize(current_alignment, true);
                    }
                    else
                    {
                        current_alignment += ((TypeCode)member.getTypecode()).maxSerializedSize(current_alignment);
                    }
                }
            }
            else if (!only_keys)
            {
                current_alignment += ((TypeCode)member.getTypecode()).maxSerializedSize(current_alignment);
            }
        }

        return current_alignment - initial_alignment;
    }

    public String getMaxKeySerializedSize()
    {
        return Long.toString(maxSerializedSize(0, true));
    }

    public void setIsTopic(
            boolean value)
    {
        istopic_ = value;
    }

    public boolean isIsTopic()
    {
        return istopic_;
    }

    public ArrayList<String> getNamespaces()
    {
        ArrayList<String> namespaces = new ArrayList<String>();
        String scopes = getScope();
        int ch_pos = scopes.indexOf("::");

        while (0 < ch_pos)
        {
            namespaces.add(scopes.substring(0, ch_pos));
            scopes = scopes.substring(ch_pos + 2);
            ch_pos = scopes.indexOf("::");
        }

        if (!scopes.isEmpty())
        {
            namespaces.add(scopes);
        }

        return namespaces;
    }

    private boolean istopic_ = true;
}
