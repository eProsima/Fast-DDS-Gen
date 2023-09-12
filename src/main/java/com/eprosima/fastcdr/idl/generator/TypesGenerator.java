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

package com.eprosima.fastcdr.idl.generator;

import com.eprosima.idl.context.Context;
import com.eprosima.idl.generator.manager.TemplateManager;
import com.eprosima.idl.parser.tree.AnnotationDeclaration;
import com.eprosima.idl.parser.tree.Definition;
import com.eprosima.idl.parser.tree.Export;
import com.eprosima.idl.parser.tree.Interface;
import com.eprosima.idl.parser.tree.TypeDeclaration;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.log.ColorMessage;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;



public class TypesGenerator
{
    public TypesGenerator(TemplateManager tmanager, String outputDir, boolean replace)
    {
        tmanager_ = tmanager;
        outputDir_ = outputDir;
        replace_ = replace;
        stg_ = tmanager_.createStringTemplateGroup("JavaType");
    }

    /*!
     * @brief This function generates data types in Java.
     * It uses a context that was processed by the IDL parser.
     */
    public boolean generate(Context context, String packagDir, String packag, String libraryName, Map<String, String> extensions)
    {
        ArrayList<Definition> definitions = context.getDefinitions();

        boolean returnedValue = processDefinitions(context, definitions, packagDir, packag, extensions);

        if(returnedValue)
        {
            // Create gradle build script.
            STGroup gradlestg = tmanager_.createStringTemplateGroup("gradle");
            ST gradlest = gradlestg.getInstanceOf("main");
            gradlest.add("name", libraryName);

            if(!writeFile(outputDir_ + "build.gradle", gradlest))
            {
                System.out.println(ColorMessage.error() + "Cannot write file " + outputDir_ + "build.gradle");
                returnedValue = false;
            }
        }

        return returnedValue;
    }

    public boolean processDefinitions(Context context, ArrayList<Definition> definitions, String packagDir, String packag, Map<String, String> extensions)
    {
        if(definitions != null)
        {
            for(Definition definition : definitions)
            {
                if(definition.isIsModule())
                {
                    com.eprosima.idl.parser.tree.Module module = (com.eprosima.idl.parser.tree.Module)definition;

                    // Create directory for module.
                    String outputDir = packagDir  + module.getName();
                    File dir = new File(outputDir);

                    if(!dir.exists())
                    {
                        if(!dir.mkdir())
                        {
                            System.out.println(ColorMessage.error() + "Cannot create directory for module " + module.getName());
                            return false;
                        }
                    }

                    if(!processDefinitions(context, module.getDefinitions(), outputDir + File.separator,
                            packag + "." + module.getName(), extensions))
                        return false;
                }
                else if(definition.isIsInterface())
                {
                    Interface ifc = (Interface)definition;

                    // Create ST of the interface
                    ST ifcst = stg_.getInstanceOf("interface");
                    ifcst.add("ctx", context);
                    ifcst.add("parent", ifc.getParent());
                    ifcst.add("interface", ifc);

                    ST extensionst = null;
                    String extensionname = null;
                    if(extensions != null && (extensionname = extensions.get("interface")) != null)
                    {
                        extensionst = stg_.getInstanceOf(extensionname);
                        extensionst.add("ctx", context);
                        extensionst.add("parent", ifc.getParent());
                        extensionst.add("interface", ifc);
                        ifcst.add("extension", extensionst.render());
                    }

                    if(processExports(context, ifc.getExports(), ifcst, extensions))
                    {
                        // Save file.
                        ST st = stg_.getInstanceOf("main");
                        st.add("ctx", context);
                        st.add("definitions", ifcst.render());
                        st.add("package", (!packag.isEmpty() ? packag : null));

                        if(extensions != null && (extensionname = extensions.get("main")) != null)
                        {
                            extensionst = stg_.getInstanceOf(extensionname);
                            extensionst.add("ctx", context);
                            st.add("extension", extensionst.render());
                        }

                        if(!writeFile(packagDir + ifc.getName() + ".java", st))
                        {
                            System.out.println(ColorMessage.error() + "Cannot write file " + packagDir + ifc.getName() + ".java");
                            return false;
                        }
                    }
                    else
                        return false;
                }
                else if(definition.isIsTypeDeclaration())
                {
                    TypeDeclaration typedecl = (TypeDeclaration)definition;

                    // get ST of the structure
                    ST typest = processTypeDeclaration(context, typedecl, extensions);

                    if(typest != null)
                    {
                        // Save file.
                        ST st = stg_.getInstanceOf("main");
                        st.add("ctx", context);
                        st.add("definitions", typest.render());
                        st.add("package", (!packag.isEmpty() ? packag : null));

                        ST extensionst = null;
                        String extensionname = null;
                        if(extensions != null && (extensionname = extensions.get("main")) != null)
                        {
                            extensionst = stg_.getInstanceOf(extensionname);
                            extensionst.add("ctx", context);
                            st.add("extension", extensionst.render());
                        }

                        if(!writeFile(packagDir + typedecl.getName() + ".java", st))
                        {
                            System.out.println(ColorMessage.error() + "Cannot write file " + packagDir + typedecl.getName() + ".java");
                            return false;
                        }
                    }
                }
                else if(definition.isIsAnnotation())
                {
                    AnnotationDeclaration annotation = (AnnotationDeclaration)definition;

                    // Create ST of the annotation
                    ST ifcst = stg_.getInstanceOf("annotation");
                    ifcst.add("ctx", context);
                    ifcst.add("annotation", annotation);

                    ST extensionst = null;
                    String extensionname = null;
                    if(extensions != null && (extensionname = extensions.get("annotation")) != null)
                    {
                        extensionst = stg_.getInstanceOf(extensionname);
                        extensionst.add("ctx", context);
                        extensionst.add("annotation", annotation);
                        ifcst.add("extension", extensionst.render());
                    }
                }
            }
        }

        return true;
    }

    public boolean processExports(Context context, ArrayList<Export> exports, ST ifcst, Map<String, String> extensions)
    {
        for(Export export : exports)
        {
            if(export.isIsTypeDeclaration())
            {
                TypeDeclaration typedecl = (TypeDeclaration)export;

                // get ST of the structure
                ST typest = processTypeDeclaration(context, typedecl, extensions);

                if(typest != null)
                {
                    // Add type stringtemplate to interface stringtemplate.
                    ifcst.add("exports", typest.render());
                }
            }
        }

        return true;
    }

    public ST processTypeDeclaration(Context context, TypeDeclaration typedecl, Map<String, String> extensions)
    {
        ST typest = null, extensionst = null;
        String extensionname = null;
        System.out.println("processTypesDeclaration " + typedecl.getName());

        if(typedecl.getTypeCode().getKind() == Kind.KIND_STRUCT)
        {
            typest = stg_.getInstanceOf("struct_type");
            typest.add("struct", typedecl.getTypeCode());

            // Get extension
            if(extensions != null && (extensionname =  extensions.get("struct_type")) != null)
            {
                extensionst = stg_.getInstanceOf(extensionname);
                extensionst.add("struct", typedecl.getTypeCode());
            }
        }
        else if(typedecl.getTypeCode().getKind() == Kind.KIND_UNION)
        {
            typest = stg_.getInstanceOf("union_type");
            typest.add("union", typedecl.getTypeCode());

            // Get extension
            if(extensions != null && (extensionname =  extensions.get("union_type")) != null)
            {
                extensionst = stg_.getInstanceOf(extensionname);
                extensionst.add("union", typedecl.getTypeCode());
            }
        }
        else if(typedecl.getTypeCode().getKind() == Kind.KIND_ENUM)
        {
            typest = stg_.getInstanceOf("enum_type");
            typest.add("enum", typedecl.getTypeCode());

            // Get extension
            if(extensions != null && (extensionname =  extensions.get("enum_type")) != null)
            {
                extensionst = stg_.getInstanceOf(extensionname);
                extensionst.add("enum", typedecl.getTypeCode());
            }
        }
        else if(typedecl.getTypeCode().getKind() == Kind.KIND_BITSET)
        {
            typest = stg_.getInstanceOf("bitset_type");
            typest.add("bitset", typedecl.getTypeCode());

            // Get extension
            if(extensions != null && (extensionname =  extensions.get("bitset_type")) != null)
            {
                extensionst = stg_.getInstanceOf(extensionname);
                extensionst.add("bitset", typedecl.getTypeCode());
            }
        }
        else if(typedecl.getTypeCode().getKind() == Kind.KIND_BITMASK)
        {
            typest = stg_.getInstanceOf("bitmask_type");
            typest.add("bitmask", typedecl.getTypeCode());

            // Get extension
            if(extensions != null && (extensionname =  extensions.get("bitmask_type")) != null)
            {
                extensionst = stg_.getInstanceOf(extensionname);
                extensionst.add("bitmask", typedecl.getTypeCode());
            }
        }

        if(typest != null)
        {
            // Generate extension
            if(extensionst != null)
            {
                extensionst.add("ctx", context);
                extensionst.add("parent", typedecl.getParent());
                typest.add("extension", extensionst.render());
            }

            // Main stringtemplate
            typest.add("ctx", context);
            typest.add("parent", typedecl.getParent());
        }

        return typest;
    }

    private boolean writeFile(String file, ST template)
    {
        boolean returnedValue = false;

        try
        {
            File handle = new File(file);

            if(!handle.exists() || replace_)
            {
                FileWriter fw = new FileWriter(file);
                String data = template.render();
                fw.write(data, 0, data.length());
                fw.close();
            }
            else
            {
                System.out.println("INFO: " + file + " exists. Skipping.");
            }

            returnedValue = true;
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        return returnedValue;
    }

    private TemplateManager tmanager_ = null;
    private STGroup stg_ = null;
    private String outputDir_ = null;
    private boolean replace_ = false;
}
