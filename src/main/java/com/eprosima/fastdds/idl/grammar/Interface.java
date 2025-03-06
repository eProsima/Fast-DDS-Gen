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

import com.eprosima.fastdds.idl.grammar.Operation;
import org.antlr.v4.runtime.Token;

public class Interface extends com.eprosima.idl.parser.tree.Interface
{
    public Interface(Context ctx, String scopeFile, boolean isInScope, String scope, String name, Token tk)
    {
        super(scopeFile, isInScope, scope, name, tk);
        m_context = ctx;
    }

    @Override
    public void add(com.eprosima.idl.parser.tree.Export exp)
    {
        if (exp instanceof Operation)
        {
            Operation op = (Operation)exp;
            if (op.isAnnotationFeed())
            {
                m_context.setThereIsOutputFeed(true);
            }
        }

        super.add(exp);
    }

    private Context m_context;
}
