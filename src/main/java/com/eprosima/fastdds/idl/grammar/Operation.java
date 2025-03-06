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

import com.eprosima.idl.parser.exception.RuntimeGenerationException;
import com.eprosima.idl.parser.exception.ParseException;
import com.eprosima.fastdds.idl.grammar.Param;
import org.antlr.v4.runtime.Token;

public class Operation extends com.eprosima.idl.parser.tree.Operation
{
    public Operation(Context ctx, String scopeFile, boolean isInScope, String scope, String name, Token tk)
    {
        super(scopeFile, isInScope, scope, name, tk);
        m_context = ctx;
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

    @Override
    public void add(com.eprosima.idl.parser.tree.Param param)
    {
        Param p = (Param)param;
        // Process feed annotation
        if (p.isAnnotationFeed())
        {
            if (p.isOutput())
            {
                // Fail if parameter is out and feed
                throw new ParseException(null, "Output parameter " + p.getName() + " has '@feed' annotation.");
            }
            else
            {
                // Take note that there is at least one input feed
                m_context.setThereIsInputFeed(true);
            }
        }

        super.add(param);
    }

    private Context m_context;
}
