// Copyright 2025 Proyectos y Sistemas de Mantenimiento SL (eProsima).
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

package com.eprosima.fastdds.idl.grammar;

import com.eprosima.fastdds.idl.parser.typecode.StructTypeCode;
import com.eprosima.idl.parser.exception.RuntimeGenerationException;
import com.eprosima.idl.parser.typecode.TypeCode;

import org.antlr.v4.runtime.Token;

public class Param extends com.eprosima.idl.parser.tree.Param
{
    public Param(String name, TypeCode typecode, Kind kind)

    {
        super(name, typecode, kind);
    }

    /**
     * Return whether the operation has been annotated as feed
     *
     * @return true when the operation has been annotated as feed, false otherwise.
     */
    public boolean isAnnotationFeed()
    {
        com.eprosima.idl.parser.tree.Annotation ann = getAnnotations().get(Annotation.rpc_feed_str);
        if (ann != null)
        {
            try
            {
                return ann.getValue().toUpperCase().equals(Annotation.capitalized_true_str);
            }
            catch (RuntimeGenerationException ex)
            {
                // Should not be called as @feed annotation has only one parameter
            }
        }
        return false;
    }

    public StructTypeCode getFeedTypeCode()
    {
        if (m_feedTypeCode == null)
        {
            m_feedTypeCode = ((Operation) getParent()).createFeedTypeCode(this);
        }
        return m_feedTypeCode;
    }

    private StructTypeCode m_feedTypeCode = null;
}
