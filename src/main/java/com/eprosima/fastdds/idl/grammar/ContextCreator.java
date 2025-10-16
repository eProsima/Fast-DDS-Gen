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

import java.util.ArrayList;

import com.eprosima.idl.generator.manager.TemplateManager;

/**
 * This interface provides a method to create Context instances.
 * It is used to decouple the creation of Context objects from their usage,
 * allowing for more flexible and testable code.
 */
public class ContextCreator implements ContextCreatorInterface
{
    /**
     * Creates and returns a new Context instance.
     *
     * @param tmanager The TemplateManager to be used by the Context.
     * @param file The name of the IDL file being processed.
     * @param includePaths A list of include paths for resolving imports.
     * @param subscribercode A flag indicating if subscriber code should be generated.
     * @param publishercode A flag indicating if publisher code should be generated.
     * @param appProduct The application product name.
     * @param generate_typesc A flag indicating if type support code should be generated.
     * @param generate_type_ros2 A flag indicating if ROS2 type support code should be generated.
     * @param is_generating_api A flag indicating if internal API code is being generated.
     * @param generate_typeobjectsupport A flag indicating if type object support code should be generated.
     *
     * @return A new Context object.
     */
    public Context createContext(
            TemplateManager tmanager,
            String file,
            ArrayList<String> includePaths,
            boolean subscribercode,
            boolean publishercode,
            String appProduct,
            boolean generate_typesc,
            boolean generate_type_ros2,
            boolean is_generating_api,
            boolean generate_typeobjectsupport)
    {
        return new Context(
            tmanager,
            file,
            includePaths,
            subscribercode,
            publishercode,
            appProduct,
            generate_typesc,
            generate_type_ros2,
            is_generating_api,
            generate_typeobjectsupport);
    }
}
