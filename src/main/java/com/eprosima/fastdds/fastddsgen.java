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

package com.eprosima.fastdds;

import com.eprosima.fastcdr.idl.generator.TypesGenerator;
import com.eprosima.fastdds.exceptions.BadArgumentException;
import com.eprosima.fastdds.idl.grammar.Context;
import com.eprosima.fastdds.solution.Project;
import com.eprosima.fastdds.solution.Solution;
import com.eprosima.fastdds.util.Utils;
import com.eprosima.fastdds.util.VSConfiguration;
import com.eprosima.idl.generator.manager.TemplateExtension;
import com.eprosima.idl.generator.manager.TemplateGroup;
import com.eprosima.idl.generator.manager.TemplateManager;
import com.eprosima.idl.parser.grammar.IDLLexer;
import com.eprosima.idl.parser.grammar.IDLParser;
import com.eprosima.idl.parser.tree.AnnotationDeclaration;
import com.eprosima.idl.parser.tree.AnnotationMember;
import com.eprosima.idl.parser.tree.Specification;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.idl.parser.typecode.TypeCode;
import com.eprosima.idl.util.Util;
import com.eprosima.log.ColorMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;




// TODO: Implement Solution & Project in com.eprosima.fastdds.solution

public class fastddsgen
{

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Attributes
     */

    private static ArrayList<String> m_platforms = null;

    private Vector<String> m_idlFiles;
    protected static String m_appEnv = "FASTRTPSHOME";
    private String m_exampleOption = null;
    private boolean m_ppDisable = false; //TODO
    private boolean m_replace = false;
    private String m_ppPath = null;
    private final String m_defaultOutputDir = "." + File.separator;
    private String m_outputDir = m_defaultOutputDir;
    private String m_tempDir = null;
    protected static String m_appName = "fastddsgen";

    private boolean m_publishercode = true;
    private boolean m_subscribercode = true;
    private boolean m_atLeastOneStructure = false;
    protected static String m_localAppProduct = "fastrtps";
    private ArrayList<String> m_includePaths = new ArrayList<String>();

    // Mapping where the key holds the path to the template file and the value the wanted output file name
    private Map<String, String> m_customStgOutput = new HashMap<String, String>();

    private static VSConfiguration m_vsconfigurations[] = {
        new VSConfiguration("Debug DLL", "Win32", true, true),
        new VSConfiguration("Release DLL", "Win32", false, true),
        new VSConfiguration("Debug", "Win32", true, false),
        new VSConfiguration("Release", "Win32", false, false)
    };

    private String m_os = null;
    private boolean fusion_ = false;

    //! Default package used in Java files.
    private String m_package = "";

    // Generates type naming compatible with ROS 2
    private boolean m_type_ros2 = false;

    // Generate TypeObject files?
    private boolean m_type_object_files = false;

    // Generate string and sequence types compatible with C?
    private boolean m_typesc = false;

    // Generate python binding files
    private boolean m_python = false;

    // Generate json support files
    private boolean m_json_files = false;

    private boolean m_case_sensitive = false;

    // Testing
    private boolean m_test = false;

    // Use to know the programming language
    public enum LANGUAGE
    {
        CPP,
        JAVA
    };

    private LANGUAGE m_languageOption = LANGUAGE.CPP; // Default language -> CPP

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Constructor
     */

    public fastddsgen(
            String [] args) throws BadArgumentException
    {

        int count = 0;
        String arg;

        // Detect OS
        m_os = System.getProperty("os.name");

        m_idlFiles = new Vector<String>();

        // Check arguments
        while (count < args.length)
        {

            arg = args[count++];

            if (!arg.startsWith("-"))
            {
                m_idlFiles.add(arg);
            }
            else if (arg.equals("-example"))
            {
                if (count < args.length)
                {
                    m_exampleOption = args[count++];
                    if (!m_platforms.contains(m_exampleOption))
                    {
                        throw new BadArgumentException("Unknown example arch " + m_exampleOption);
                    }
                }
                else
                {
                    throw new BadArgumentException("No architecture speficied after -example argument");
                }
            }
            else if (arg.equals("-language"))
            {
                if (count < args.length)
                {
                    String languageOption = args[count++];

                    if (languageOption.equalsIgnoreCase("c++"))
                    {
                        m_languageOption = LANGUAGE.CPP;
                    }
                    else if (languageOption.equalsIgnoreCase("java"))
                    {
                        m_languageOption = LANGUAGE.JAVA;
                    }
                    else
                    {
                        throw new BadArgumentException("Unknown language " +  languageOption);
                    }
                }
                else
                {
                    throw new BadArgumentException("No language specified after -language argument");
                }
            }
            else if (arg.equals("-package"))
            {
                if (count < args.length)
                {
                    m_package = args[count++];
                }
                else
                {
                    throw new BadArgumentException("No package after -package argument");
                }
            }
            else if (arg.equals("-ppPath"))
            {
                if (count < args.length)
                {
                    m_ppPath = args[count++];
                }
                else
                {
                    throw new BadArgumentException("No URL specified after -ppPath argument");
                }
            }
            else if (arg.equals("-extrastg"))
            {
                if (count + 1 < args.length)
                {
                    m_customStgOutput.put(args[count++], args[count++]);
                }
                else
                {
                    throw new BadArgumentException("Missing arguments for -extrastg");
                }
            }
            else if (arg.equals("-ppDisable"))
            {
                m_ppDisable = true;
            }
            else if (arg.equals("-replace"))
            {
                m_replace = true;
            }
            else if (arg.equals("-d"))
            {
                if (count < args.length)
                {
                    m_outputDir = Utils.addFileSeparator(args[count++]);
                }
                else
                {
                    throw new BadArgumentException("No URL specified after -d argument");
                }
            }
            else if (arg.equals("-t"))
            {
                if (count < args.length)
                {
                    m_tempDir = Utils.addFileSeparator(args[count++]);
                }
                else
                {
                    throw new BadArgumentException("No temporary directory specified after -t argument");
                }
            }
            else if (arg.equals("-version"))
            {
                showVersion();
                System.exit(0);
            }
            else if (arg.equals("-help"))
            {
                printHelp();
                System.exit(0);
            }
            else if (arg.equals("-fusion"))
            {
                fusion_ = true;
            }
            else if (arg.equals("-typeros2"))
            {
                m_type_ros2 = true;
            }
            else if (arg.equals("-typeobject"))
            {
                m_type_object_files = true;
            }
            else if (arg.equals("-typesc"))
            {
                m_typesc = true;
            }
            else if (arg.equals("-python"))
            {
                m_python = true;
            }
            else if (arg.equals("-json"))
            {
                m_json_files = true;
            }
            else if (arg.equals("-test"))
            {
                m_test = true;
            }
            else if (arg.equals("-I"))
            {
                if (count < args.length)
                {
                    m_includePaths.add("-I".concat(args[count++]));
                }
                else
                {
                    throw new BadArgumentException("No include directory specified after -I argument");
                }
            }
            else if (arg.equals("-cs"))
            {
                m_case_sensitive = true;
            }
            else   // TODO: More options: -rpm, -debug
            {
                throw new BadArgumentException("Unknown argument " + arg);
            }

        }

        if (null != m_exampleOption && m_python)
        {
            throw new BadArgumentException("-example and -python currently are incompatible");
        }

        if (m_idlFiles.isEmpty())
        {
            throw new BadArgumentException("No input files given");
        }

    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Listener classes
     */

    class TemplateErrorListener implements StringTemplateErrorListener
    {
        public void error(
                String arg0,
                Throwable arg1)
        {
            System.out.println(ColorMessage.error() + arg0);
            arg1.printStackTrace();
        }

        public void warning(
                String arg0)
        {
            System.out.println(ColorMessage.warning() + arg0);
        }

    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main methods
     */

    public boolean execute()
    {


        if (!m_outputDir.equals(m_defaultOutputDir))
        {
            File dir = new File(m_outputDir);

            if (!dir.exists())
            {
                System.out.println(ColorMessage.error() + "The specified output directory does not exist");
                return false;
            }
        }

        boolean returnedValue = globalInit();

        if (returnedValue)
        {
            Solution solution = new Solution(m_languageOption, m_exampleOption,
                            getVersion(), m_publishercode, m_subscribercode);

            // Load string templates
            System.out.println("Loading templates from " + System.getProperty("java.class.path"));
            // Add path of custom templates to manager search path
            String extraPaths = "";
            for (Map.Entry<String, String> entry : m_customStgOutput.entrySet())
            {
                Path path = Paths.get(entry.getKey()).getParent();
                if (path != null)
                {
                    extraPaths += ":" + path.toString().replace("\\", "/");
                }
                else
                {
                    extraPaths += ":./";
                }
            }

            String templatePaths = "com/eprosima/fastdds/idl/templates:com/eprosima/fastcdr/idl/templates" + extraPaths;
            System.out.println("Template resource folders: " + templatePaths);
            TemplateManager.setGroupLoaderDirectories(templatePaths);

            // In local for all products
            if (m_os.contains("Windows"))
            {
                solution.addInclude("$(" + m_appEnv + ")/include");
                solution.addLibraryPath("$(" + m_appEnv + ")/lib");
                if (m_exampleOption != null)
                {
                    solution.addLibraryPath("$(" + m_appEnv + ")/lib/" + m_exampleOption);
                    solution.addLibraryPath("$(" + m_appEnv + ")/lib/" + m_exampleOption + "/VC/static");
                }
            }

            // If Java, include jni headers
            if (m_languageOption == LANGUAGE.JAVA)
            {
                solution.addInclude("$(JAVA_HOME)/include");

                if (m_exampleOption != null && m_exampleOption.contains("Linux"))
                {
                    solution.addInclude("$(JAVA_HOME)/include/linux");
                }
            }

            if ((m_exampleOption != null || m_test) && !m_exampleOption.contains("Win"))
            {
                solution.addLibrary("fastcdr");
            }

            // Add product library
            solution.addLibrary("fastrtps");

            for (int count = 0; returnedValue && (count < m_idlFiles.size()); ++count)
            {
                Project project = process(m_idlFiles.get(count), null, true);

                if (project != null)
                {
                    System.out.println("Adding project: " + project.getFile());
                    if (!solution.existsProject(project.getFile()))
                    {
                        solution.addProject(project);
                    }
                }
                else
                {
                    returnedValue = false;
                }

                for (String include : project.getIDLIncludeFiles())
                {
                    Project inner = process(include, Util.getIDLFileDirectoryOnly(m_idlFiles.get(count)), false);
                    if (inner != null && !solution.existsProject(inner.getFile()))
                    {
                        System.out.println("Adding project: " + inner.getFile());
                        solution.addProject(inner);
                    }
                }
            }

            if (returnedValue && m_python)
            {
                returnedValue = genSwigCMake(solution);
            }


            // Generate solution
            if (returnedValue && (m_exampleOption != null) || m_test)
            {
                if ((returnedValue = genSolution(solution)) == false)
                {
                    System.out.println(ColorMessage.error() + "While the solution was being generated");
                }
            }

        }

        return returnedValue;

    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Auxiliary methods
     */

    public static boolean loadPlatforms()
    {

        boolean returnedValue = false;

        fastddsgen.m_platforms = new ArrayList<String>();

        fastddsgen.m_platforms.add("i86Win32VS2019");
        fastddsgen.m_platforms.add("x64Win64VS2019");
        fastddsgen.m_platforms.add("i86Linux2.6gcc");
        fastddsgen.m_platforms.add("x64Linux2.6gcc");
        fastddsgen.m_platforms.add("armLinux2.6gcc");
        fastddsgen.m_platforms.add("CMake");

        returnedValue = true;

        return returnedValue;
    }

    private String getVersion()
    {
        try
        {
            //InputStream input = this.getClass().getResourceAsStream("/fastrtps_version.h");

            InputStream input = this.getClass().getClassLoader().getResourceAsStream("version");
            byte[] b = new byte[input.available()];
            input.read(b);
            String text = new String(b);
            int beginindex = text.indexOf("=");
            return text.substring(beginindex + 1);
        }
        catch (Exception ex)
        {
            System.out.println(ColorMessage.error() + "Getting version. " + ex.getMessage());
        }

        return "";
    }

    private void showVersion()
    {
        String version = getVersion();
        System.out.println(m_appName + " version " + version);
    }

    public static void printHelp()
    {
        System.out.println(m_appName + " usage:");
        System.out.println("\t" + m_appName + " [options] <file> [<file> ...]");
        System.out.println("\twhere the options are:");
        System.out.println("\t\t-help: shows this help");
        System.out.println("\t\t-version: shows the current version of eProsima Fast DDS gen.");
        System.out.println(
            "\t\t-example <platform>: Generates a solution for a specific platform (example: x64Win64VS2019)");
        System.out.println("\t\t\tSupported platforms:");
        for (int count = 0; count < m_platforms.size(); ++count)
        {
            System.out.println("\t\t\t * " + m_platforms.get(count));
        }
        //System.out.println("\t\t-language <C++>: Programming language (default: C++).");
        System.out.println("\t\t-replace: replaces existing generated files.");
        System.out.println("\t\t-ppDisable: disables the preprocessor.");
        System.out.println("\t\t-ppPath: specifies the preprocessor path.");
        System.out.println("\t\t-extrastg <template file> <output file name>: specifies a custom template, template location must be in classpath.");
        System.out.println("\t\t-typeros2: generates type naming compatible with ROS2.");
        System.out.println("\t\t-I <path>: add directory to preprocessor include paths.");
        System.out.println("\t\t-d <path>: sets an output directory for generated files.");
        System.out.println("\t\t-t <temp dir>: sets a specific directory as a temporary directory.");
        System.out.print("\t\t-typeobject: generates TypeObject files to automatically register the types as");
        System.out.println(" dynamic.");
        System.out.println("\t\t-cs: IDL grammar apply case sensitive matching.");
        System.out.println("\t\t-test: executes FastDDSGen tests.");
        System.out.println("\t\t-python: generates python bindings for the generated types.");
        System.out.println("\t\t-json: generates json-to-type support for the generated types.");
        System.out.println("\tand the supported input files are:");
        System.out.println("\t* IDL files.");

    }

    public boolean globalInit()
    {

        // Set the temporary folder
        if (m_tempDir == null)
        {
            if (m_os.contains("Windows"))
            {
                String tempPath = System.getenv("TEMP");

                if (tempPath == null)
                {
                    tempPath = System.getenv("TMP");
                }

                m_tempDir = tempPath;
            }
            else if (m_os.contains("Linux") || m_os.contains("Mac"))
            {
                m_tempDir = "/tmp/";
            }
        }

        if (m_tempDir.charAt(m_tempDir.length() - 1) != File.separatorChar)
        {
            m_tempDir += File.separator;
        }

        return true;
    }

    private Project process(
            String idlFilename,
            String dependant_idl_dir,
            boolean processCustomTemplates)
    {
        Project project = null;
        System.out.println("Processing the file " + idlFilename + "...");

        try
        {
            // Protocol CDR
            project = parseIDL(idlFilename, dependant_idl_dir, processCustomTemplates); // TODO: Quitar archivos copiados TypesHeader.stg, TypesSource.stg, PubSubTypeHeader.stg de la carpeta com.eprosima.fastdds.idl.templates
        }
        catch (Exception ioe)
        {
            System.out.println(ColorMessage.error() + "Cannot generate the files");
            if (!ioe.getMessage().equals(""))
            {
                System.out.println(ioe.getMessage());
            }
        }

        return project;

    }

    private Project parseIDL(
            String idlFilename,
            String dependant_idl_dir,
            boolean processCustomTemplates)
    {
        boolean returnedValue = false;
        String idlParseFileName = idlFilename;
        Project project = null;

        if (!m_ppDisable)
        {
            idlParseFileName = callPreprocessor(idlFilename);
        }

        if (idlParseFileName != null)
        {
            Context ctx = new Context(idlFilename, m_includePaths, m_subscribercode, m_publishercode,
                            m_localAppProduct, m_type_object_files, m_typesc, m_type_ros2);

            String relative_dir = ctx.getRelativeDir(dependant_idl_dir);
            String output_dir = m_outputDir + relative_dir;

            // Check the output directory exists or create it.
            File dir = new File(output_dir);

            if (!dir.exists())
            {
                if (!dir.mkdirs())
                {
                    System.out.println(ColorMessage.error() + "Directory " + output_dir + " cannot be created");
                    return null;
                }
            }


            if (m_case_sensitive)
            {
                ctx.ignore_case(false);
            }

            if (fusion_)
            {
                ctx.setActivateFusion(true);
            }

            // Create default @Key annotation.
            AnnotationDeclaration keyann = ctx.createAnnotationDeclaration("Key", null);
            keyann.addMember(new AnnotationMember("value", new PrimitiveTypeCode(Kind.KIND_BOOLEAN), "true"));

            // Create default @Topic annotation.
            AnnotationDeclaration topicann = ctx.createAnnotationDeclaration("Topic", null);
            topicann.addMember(new AnnotationMember("value", new PrimitiveTypeCode(Kind.KIND_BOOLEAN), "true"));

            // Create template manager
            TemplateManager tmanager = new TemplateManager("FastCdrCommon:eprosima:Common", ctx, m_typesc);

            List<TemplateExtension> extensions = new ArrayList<TemplateExtension>();

            // Load common types template
            /// Add extension for @key related function definitions for each struct_type.
            extensions.add(new TemplateExtension("struct_type", "keyFunctionHeadersStruct"));
            tmanager.addGroup("TypesHeader", extensions);
            if (m_type_object_files)
            {
                tmanager.addGroup("TypeObjectHeader", extensions);
            }
            extensions.clear();
            /// Add extension for @key related function declarations for each struct_type.
            extensions.add(new TemplateExtension("struct_type", "keyFunctionSourcesStruct"));
            tmanager.addGroup("TypesSource", extensions);
            if (m_type_object_files)
            {
                tmanager.addGroup("TypeObjectSource", extensions);
            }
            extensions.clear();
            /// Add extension for @key related preprocessor definitions in main for each struct typecode.
            extensions.add(new TemplateExtension("main", "keyFunctionSourcesMain"));
            tmanager.addGroup("TypesSource", extensions);

            // Load Types common templates
            tmanager.addGroup("DDSPubSubTypeHeader");
            tmanager.addGroup("DDSPubSubTypeSource");

            // Load Publisher templates
            tmanager.addGroup("DDSPublisherHeader");
            tmanager.addGroup("DDSPublisherSource");

            // Load Subscriber templates
            tmanager.addGroup("DDSSubscriberHeader");
            tmanager.addGroup("DDSSubscriberSource");

            // Load PubSubMain template
            tmanager.addGroup("DDSPubSubMain");

            if (m_test)
            {
                // Load test template
                tmanager.addGroup("SerializationTestSource");
                tmanager.addGroup("SerializationHeader");
                tmanager.addGroup("SerializationSource");
            }

            if (m_json_files)
            {
                tmanager.addGroup("JsonSupportHeader");
                tmanager.addGroup("JsonSupportSource");
            }

            // Add JNI sources.
            if (m_languageOption == LANGUAGE.JAVA)
            {
                tmanager.addGroup("JNIHeader");
                tmanager.addGroup("JNISource");
                tmanager.addGroup("JavaSource");

                // Set package in context.
                ctx.setPackage(m_package);
            }

            if (m_python)
            {
                tmanager.addGroup("TypesSwigInterface");
                tmanager.addGroup("DDSPubSubTypeSwigInterface");
            }

            // Load custom templates into manager
            if (processCustomTemplates)
            {
                for (Map.Entry<String, String> entry : m_customStgOutput.entrySet())
                {
                    System.out.println("Loading custom template " + entry.getKey() + "...");
                    Path path = Paths.get(entry.getKey());
                    String templateName = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
                    tmanager.addGroup(templateName);
                }
            }

            // Create main template
            TemplateGroup maintemplates = tmanager.createTemplateGroup("main");
            maintemplates.setAttribute("ctx", ctx);

            try
            {
                ANTLRFileStream input = new ANTLRFileStream(idlParseFileName);
                IDLLexer lexer = new IDLLexer(input);
                lexer.setContext(ctx);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                IDLParser parser = new IDLParser(tokens);
                // Pass the filename without the extension

                Specification specification = parser.specification(ctx, tmanager, maintemplates).spec;
                returnedValue = specification != null;

            }
            catch (FileNotFoundException ex)
            {
                System.out.println(ColorMessage.error(
                            "FileNotFounException") + "The File " + idlParseFileName + " was not found.");
            }/* catch (ParseException ex) {
                System.out.println(ColorMessage.error("ParseException") + ex.getMessage());
                }*/
            catch (Exception ex)
            {
                System.out.println(ColorMessage.error("Exception") + ex.getMessage());
            }

            if (returnedValue)
            {
                // Create information of project for solution
                project = new Project(ctx, idlFilename, ctx.getDependencies());

                // Create all custom files for template
                if (processCustomTemplates)
                {
                    for (Map.Entry<String, String> entry : m_customStgOutput.entrySet())
                    {
                        Path path = Paths.get(entry.getKey());
                        String templateName = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
                        System.out.println("Generating from custom " + templateName + " to " + entry.getValue());

                        if (returnedValue = Utils.writeFile(output_dir + entry.getValue(), maintemplates.getTemplate(templateName), m_replace))
                        {
                            // Try to determine if the file is a header file.
                            if (entry.getValue().contains(".hpp") || entry.getValue().contains(".h"))
                            {
                                project.addCommonIncludeFile(relative_dir + entry.getValue());
                            }
                            else
                            {
                                project.addCommonSrcFile(relative_dir + ctx.getFilename() + entry.getValue());
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                }

                System.out.println("Generating Type definition files...");
                if ((returnedValue) && (returnedValue =
                        Utils.writeFile(output_dir + ctx.getFilename() + ".h",
                        maintemplates.getTemplate("TypesHeader"),
                        m_replace)))
                {
                    if (returnedValue =
                            Utils.writeFile(output_dir + ctx.getFilename() + ".cxx",
                            maintemplates.getTemplate("TypesSource"), m_replace))
                    {
                        project.addCommonIncludeFile(relative_dir + ctx.getFilename() + ".h");
                        project.addCommonSrcFile(relative_dir + ctx.getFilename() + ".cxx");
                        if (m_type_object_files)
                        {
                            System.out.println("Generating TypeObject files...");
                            if (returnedValue = Utils.writeFile(output_dir + ctx.getFilename() + "TypeObject.h",
                                    maintemplates.getTemplate("TypeObjectHeader"), m_replace))
                            {
                                if (returnedValue = Utils.writeFile(output_dir + ctx.getFilename() + "TypeObject.cxx",
                                        maintemplates.getTemplate("TypeObjectSource"), m_replace))
                                {
                                    project.addCommonIncludeFile(relative_dir + ctx.getFilename() + "TypeObject.h");
                                    project.addCommonSrcFile(relative_dir + ctx.getFilename() + "TypeObject.cxx");
                                }
                            }
                        }
                        if (m_python)
                        {
                            System.out.println("Generating Swig interface files...");
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + ".i",
                                    maintemplates.getTemplate("TypesSwigInterface"), m_replace))
                            {

                            }
                        }
                        
                        if (m_json_files)
                        {
                            System.out.println("Generating json support files...");
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "JsonSupport.h",
                                    maintemplates.getTemplate("JsonSupportHeader"), m_replace))
                            {
                                returnedValue = Utils.writeFile(output_dir + ctx.getFilename() + "JsonSupport.cxx",
                                    maintemplates.getTemplate("JsonSupportSource"), m_replace);
                            }
                        }
                        
                    }
                }

                if (m_test)
                {
                    System.out.println("Generating Serialization Test file...");
                    String fileName = output_dir + ctx.getFilename() + "SerializationTest.cpp";
                    returnedValue =
                            Utils.writeFile(fileName, maintemplates.getTemplate("SerializationTestSource"), m_replace);
                    project.addCommonTestingFile(relative_dir + ctx.getFilename() + "SerializationTest.cpp");

                    System.out.println("Generating Serialization Source file...");
                    String fileNameS = output_dir + ctx.getFilename() + "Serialization.cpp";
                    returnedValue =
                            Utils.writeFile(fileNameS, maintemplates.getTemplate("SerializationSource"), m_replace);
                    project.addCommonTestingFile(relative_dir + ctx.getFilename() + "Serialization.cpp");

                    System.out.println("Generating Serialization Header file...");
                    String fileNameH = output_dir + ctx.getFilename() + "Serialization.h";
                    returnedValue =
                            Utils.writeFile(fileNameH, maintemplates.getTemplate("SerializationHeader"), m_replace);
                    project.addCommonTestingFile(relative_dir + ctx.getFilename() + "PubSubTypes.cxx");
                    
                    for (String element : project.getFullDependencies())
                    {
                        String trimmedElement = element.substring(0, element.length() - 4);// Remove .idl
                        project.addCommonTestingFile(trimmedElement + "Serialization.cpp");
                    }
                }

                // TODO: Uncomment following lines and create templates
                if (ctx.existsLastStructure())
                {
                    m_atLeastOneStructure = true;
                    project.setHasStruct(true);

                    System.out.println("Generating TopicDataTypes files...");
                    if (returnedValue =
                            Utils.writeFile(output_dir + ctx.getFilename() + "PubSubTypes.h",
                            maintemplates.getTemplate("DDSPubSubTypeHeader"), m_replace))
                    {
                        if (returnedValue =
                                Utils.writeFile(output_dir + ctx.getFilename() + "PubSubTypes.cxx",
                                maintemplates.getTemplate("DDSPubSubTypeSource"), m_replace))
                        {
                            project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "PubSubTypes.h");
                            project.addProjectSrcFile(relative_dir + ctx.getFilename() + "PubSubTypes.cxx");
                            if (m_python)
                            {
                                System.out.println("Generating Swig interface files...");
                                returnedValue = Utils.writeFile(
                                    output_dir + ctx.getFilename() + "PubSubTypes.i",
                                    maintemplates.getTemplate("DDSPubSubTypeSwigInterface"), m_replace);
                            }
                        }
                    }

                    if (m_exampleOption != null)
                    {
                        System.out.println("Generating Publisher files...");
                        if (returnedValue =
                                Utils.writeFile(output_dir + ctx.getFilename() + "Publisher.h",
                                maintemplates.getTemplate("DDSPublisherHeader"), m_replace))
                        {
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "Publisher.cxx",
                                    maintemplates.getTemplate("DDSPublisherSource"), m_replace))
                            {
                                project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "Publisher.h");
                                project.addProjectSrcFile(relative_dir + ctx.getFilename() + "Publisher.cxx");
                            }
                        }

                        System.out.println("Generating Subscriber files...");
                        if (returnedValue =
                                Utils.writeFile(output_dir + ctx.getFilename() + "Subscriber.h",
                                maintemplates.getTemplate("DDSSubscriberHeader"), m_replace))
                        {
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "Subscriber.cxx",
                                    maintemplates.getTemplate("DDSSubscriberSource"), m_replace))
                            {
                                project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "Subscriber.h");
                                project.addProjectSrcFile(relative_dir + ctx.getFilename() + "Subscriber.cxx");
                            }
                        }

                        System.out.println("Generating main file...");
                        if (returnedValue =
                                Utils.writeFile(output_dir + ctx.getFilename() + "PubSubMain.cxx",
                                maintemplates.getTemplate("DDSPubSubMain"), m_replace))
                        {
                            project.addProjectSrcFile(relative_dir + ctx.getFilename() + "PubSubMain.cxx");
                        }
                    }
                }
            }

            // Java support (Java classes and JNI code)
            if (returnedValue && m_languageOption == LANGUAGE.JAVA)
            {
                String outputDir = output_dir;

                // Make directories from package.
                if (!m_package.isEmpty())
                {
                    outputDir = output_dir + File.separator + m_package.replace('.', File.separatorChar);
                    File dirs = new File(outputDir);

                    if (!dirs.exists())
                    {
                        if (!dirs.mkdirs())
                        {
                            System.out.println(ColorMessage.error() + "Cannot create directories for Java packages.");
                            return null;
                        }
                    }
                }

                // Java classes.
                TypesGenerator typeGen = new TypesGenerator(tmanager, output_dir, m_replace);
                TypeCode.javapackage = m_package + (m_package.isEmpty() ? "" : ".");
                if (!typeGen.generate(ctx, outputDir + File.separator, m_package, ctx.getFilename(), null))
                {
                    System.out.println(ColorMessage.error() + "generating Java types");
                    return null;
                }

                if (ctx.existsLastStructure())
                {
                    System.out.println("Generando fichero " + output_dir + ctx.getFilename() + "PubSub.java");
                    if (!Utils.writeFile(outputDir + File.separator + ctx.getFilename() + "PubSub.java",
                            maintemplates.getTemplate("JavaSource"), m_replace))
                    {
                        return null;
                    }

                    // Call javah application for each structure.
                    if (!callJavah(idlFilename))
                    {
                        return null;
                    }
                }

                if (Utils.writeFile(output_dir + ctx.getFilename() + "PubSubJNII.h",
                        maintemplates.getTemplate("JNIHeader"),
                        m_replace))
                {
                    project.addJniIncludeFile(relative_dir + ctx.getFilename() + "PubSubJNII.h");
                }
                else
                {
                    return null;
                }

                StringTemplate jnisourceTemplate = maintemplates.getTemplate("JNISource");
                if (Utils.writeFile(output_dir + ctx.getFilename() + "PubSubJNI.cxx", jnisourceTemplate, m_replace))
                {
                    project.addJniSrcFile(relative_dir + ctx.getFilename() + "PubSubJNI.cxx");
                }
                else
                {
                    return null;
                }
            }
        }

        return returnedValue ? project : null;
    }

    private boolean genSolution(
            Solution solution)
    {

        final String METHOD_NAME = "genSolution";
        boolean returnedValue = true;
        if (m_atLeastOneStructure == true)
        {
            if (m_exampleOption != null)
            {
                System.out.println("Generating solution for arch " + m_exampleOption + "...");

                if (m_exampleOption.equals("CMake") || m_test)
                {
                    System.out.println("Generating CMakeLists solution");
                    returnedValue = genCMakeLists(solution);
                }
                else if (m_exampleOption.substring(3, 6).equals("Win"))
                {
                    System.out.println("Generating Windows solution");
                    if (m_exampleOption.startsWith("i86"))
                    {
                        returnedValue = genVS(solution, null, "16", "142");
                    }
                    else if (m_exampleOption.startsWith("x64"))
                    {
                        for (int index = 0; index < m_vsconfigurations.length; index++)
                        {
                            m_vsconfigurations[index].setPlatform("x64");
                        }
                        returnedValue = genVS(solution, "x64", "16", "142");
                    }
                    else
                    {
                        returnedValue = false;
                    }
                }
                else if (m_exampleOption.substring(3, 8).equals("Linux"))
                {
                    System.out.println("Generating makefile solution");

                    if (m_exampleOption.startsWith("i86"))
                    {
                        returnedValue = genMakefile(solution, "-m32");
                    }
                    else if (m_exampleOption.startsWith("x64"))
                    {
                        returnedValue = genMakefile(solution, "-m64");
                    }
                    else if (m_exampleOption.startsWith("arm"))
                    {
                        returnedValue = genMakefile(solution, "");
                    }
                    else
                    {
                        returnedValue = false;
                    }
                }
            }
        }
        else
        {
            System.out.println(
                ColorMessage.warning() +
                "No structure found in any of the provided IDL; no example files have been generated");
        }

        return returnedValue;
    }

    private boolean genVS(
            Solution solution,
            String arch,
            String vsVersion,
            String toolset)
    {

        final String METHOD_NAME = "genVS";
        boolean returnedValue = false;

        StringTemplateGroup vsTemplates = StringTemplateGroup.loadGroup("VS", DefaultTemplateLexer.class, null);

        if (vsTemplates != null)
        {
            StringTemplate tsolution = vsTemplates.getInstanceOf("solution");
            StringTemplate tproject = vsTemplates.getInstanceOf("project");
            StringTemplate tprojectFiles = vsTemplates.getInstanceOf("projectFiles");
            StringTemplate tprojectPubSub = vsTemplates.getInstanceOf("projectPubSub");
            StringTemplate tprojectFilesPubSub = vsTemplates.getInstanceOf("projectFilesPubSub");
            StringTemplate tprojectJNI = null;
            StringTemplate tprojectFilesJNI = null;
            if (m_languageOption == LANGUAGE.JAVA)
            {
                tprojectJNI = vsTemplates.getInstanceOf("projectJNI");
                tprojectFilesJNI = vsTemplates.getInstanceOf("projectFilesJNI");
            }

            returnedValue = true;

            for (int count = 0; returnedValue && (count < solution.getProjects().size()); ++count)
            {
                Project project = (Project) solution.getProjects().get(count);

                tproject.setAttribute("solution", solution);
                tproject.setAttribute("project", project);
                tproject.setAttribute("example", m_exampleOption);
                tproject.setAttribute("vsVersion", vsVersion);
                tproject.setAttribute("toolset", toolset);

                tprojectFiles.setAttribute("project", project);
                tprojectFiles.setAttribute("vsVersion", vsVersion);

                tprojectPubSub.setAttribute("solution", solution);
                tprojectPubSub.setAttribute("project", project);
                tprojectPubSub.setAttribute("example", m_exampleOption);
                tprojectPubSub.setAttribute("vsVersion", vsVersion);
                tprojectPubSub.setAttribute("toolset", toolset);

                tprojectFilesPubSub.setAttribute("project", project);
                tprojectFilesPubSub.setAttribute("vsVersion", vsVersion);

                if (m_languageOption == LANGUAGE.JAVA)
                {
                    tprojectJNI.setAttribute("solution", solution);
                    tprojectJNI.setAttribute("project", project);
                    tprojectJNI.setAttribute("example", m_exampleOption);
                    tprojectJNI.setAttribute("vsVersion", vsVersion);
                    tprojectJNI.setAttribute("toolset", toolset);

                    tprojectFilesJNI.setAttribute("project", project);
                    tprojectFilesJNI.setAttribute("vsVersion", vsVersion);
                }

                for (int index = 0; index < m_vsconfigurations.length; index++)
                {
                    tproject.setAttribute("configurations", m_vsconfigurations[index]);
                    tprojectPubSub.setAttribute("configurations", m_vsconfigurations[index]);
                    if (m_languageOption == LANGUAGE.JAVA)
                    {
                        tprojectJNI.setAttribute("configurations", m_vsconfigurations[index]);
                    }
                }

                if (returnedValue =
                        Utils.writeFile(m_outputDir + project.getName() + "Types-" + m_exampleOption + ".vcxproj",
                        tproject, m_replace))
                {
                    if (returnedValue =
                            Utils.writeFile(m_outputDir + project.getName() + "Types-" + m_exampleOption +
                            ".vcxproj.filters", tprojectFiles, m_replace))
                    {
                        if (project.getHasStruct())
                        {
                            if (returnedValue =
                                    Utils.writeFile(m_outputDir + project.getName() + "PublisherSubscriber-" +
                                    m_exampleOption + ".vcxproj", tprojectPubSub, m_replace))
                            {
                                returnedValue = Utils.writeFile(
                                    m_outputDir + project.getName() + "PublisherSubscriber-" + m_exampleOption + ".vcxproj.filters", tprojectFilesPubSub,
                                    m_replace);
                            }
                        }
                    }
                }

                if (returnedValue && m_languageOption == LANGUAGE.JAVA)
                {
                    if (returnedValue =
                            Utils.writeFile(m_outputDir + project.getName() + "PubSubJNI-" + m_exampleOption +
                            ".vcxproj", tprojectJNI, m_replace))
                    {
                        returnedValue = Utils.writeFile(
                            m_outputDir + project.getName() + "PubSubJNI-" + m_exampleOption + ".vcxproj.filters", tprojectFilesJNI,
                            m_replace);
                    }
                }

                tproject.reset();
                tprojectFiles.reset();
                tprojectPubSub.reset();
                tprojectFilesPubSub.reset();
                if (m_languageOption == LANGUAGE.JAVA)
                {
                    tprojectJNI.reset();
                    tprojectFilesJNI.reset();
                }

            }

            if (returnedValue)
            {
                tsolution.setAttribute("solution", solution);
                tsolution.setAttribute("example", m_exampleOption);

                // Project configurations
                for (int index = 0; index < m_vsconfigurations.length; index++)
                {
                    tsolution.setAttribute("configurations", m_vsconfigurations[index]);
                }

                if (m_languageOption == LANGUAGE.JAVA)
                {
                    tsolution.setAttribute("generateJava", true);
                }

                String vsVersion_sol = "2019";
                tsolution.setAttribute("vsVersion", vsVersion_sol);

                returnedValue = Utils.writeFile(m_outputDir + "solution-" + m_exampleOption + ".sln", tsolution,
                                m_replace);
            }

        }
        else
        {
            System.out.println("ERROR<" + METHOD_NAME + ">: Cannot load the template group VS");
        }

        return returnedValue;
    }

    private boolean genMakefile(
            Solution solution,
            String arch)
    {

        boolean returnedValue = false;
        StringTemplate makecxx = null;

        StringTemplateGroup makeTemplates = StringTemplateGroup.loadGroup("makefile", DefaultTemplateLexer.class, null);

        if (makeTemplates != null)
        {
            makecxx = makeTemplates.getInstanceOf("makecxx");

            makecxx.setAttribute("solution", solution);
            makecxx.setAttribute("example", m_exampleOption);
            makecxx.setAttribute("arch", arch);

            returnedValue = Utils.writeFile(m_outputDir + "makefile_" + m_exampleOption, makecxx, m_replace);

        }

        return returnedValue;
    }

    private boolean genCMakeLists(
            Solution solution)
    {

        boolean returnedValue = false;
        StringTemplate cmake = null;

        StringTemplateGroup cmakeTemplates = StringTemplateGroup.loadGroup("CMakeLists", DefaultTemplateLexer.class,
                        null);

        if (cmakeTemplates != null)
        {
            cmake = cmakeTemplates.getInstanceOf("cmakelists");

            cmake.setAttribute("solution", solution);
            cmake.setAttribute("test", m_test);

            returnedValue = Utils.writeFile(m_outputDir + "CMakeLists.txt", cmake, m_replace);
        }
        return returnedValue;
    }

    private boolean genSwigCMake(
            Solution solution)
    {

        boolean returnedValue = false;
        StringTemplate swig = null;

        StringTemplateGroup swigTemplates =
                StringTemplateGroup.loadGroup("SwigCMake", DefaultTemplateLexer.class, null);
        if (swigTemplates != null)
        {
            swig = swigTemplates.getInstanceOf("swig_cmake");

            swig.setAttribute("solution", solution);

            returnedValue = Utils.writeFile(m_outputDir + "CMakeLists.txt", swig, m_replace);

        }
        return returnedValue;
    }

    String callPreprocessor(
            String idlFilename)
    {
        final String METHOD_NAME = "callPreprocessor";

        // Set line command.
        ArrayList<String> lineCommand = new ArrayList<String>();
        String [] lineCommandArray = null;
        String outputfile = Util.getIDLFileOnly(idlFilename) + ".cc";
        int exitVal = -1;
        OutputStream of = null;

        // Use temp directory.
        if (m_tempDir != null)
        {
            outputfile = m_tempDir + outputfile;
        }

        if (m_os.contains("Windows"))
        {
            try
            {
                of = new FileOutputStream(outputfile);
            }
            catch (FileNotFoundException ex)
            {
                System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot open file " + outputfile);
                return null;
            }
        }

        // Set the preprocessor path
        String ppPath = m_ppPath;

        if (ppPath == null)
        {
            if (m_os.contains("Windows"))
            {
                ppPath = "cl.exe";
            }
            else if (m_os.contains("Linux") || m_os.contains("Mac"))
            {
                ppPath = "cpp";
            }
        }

        // Add command
        lineCommand.add(ppPath);

        // Add the include paths given as parameters.
        for (int i = 0; i < m_includePaths.size(); ++i)
        {
            if (m_os.contains("Windows"))
            {
                lineCommand.add(((String) m_includePaths.get(i)).replaceFirst("^-I", "/I"));
            }
            else if (m_os.contains("Linux") || m_os.contains("Mac"))
            {
                lineCommand.add(m_includePaths.get(i));
            }
        }

        if (m_os.contains("Windows"))
        {
            lineCommand.add("/E");
            lineCommand.add("/C");
        }

        // Add input file.
        lineCommand.add(idlFilename);

        if (m_os.contains("Linux") || m_os.contains("Mac"))
        {
            lineCommand.add(outputfile);
        }

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, of, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch (Exception e)
        {
            System.out.println(ColorMessage.error(
                        METHOD_NAME) + "Cannot execute the preprocessor. Reason: " + e.getMessage());
            return null;
        }

        if (of != null)
        {
            try
            {
                of.close();
            }
            catch (IOException e)
            {
                System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot close file " + outputfile);
            }

        }

        if (exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "Preprocessor return an error " + exitVal);
            return null;
        }

        return outputfile;
    }

    boolean callJavah(
            String idlFilename)
    {
        final String METHOD_NAME = "calljavah";
        // Set line command.
        ArrayList<String> lineCommand = new ArrayList<String>();
        String[] lineCommandArray = null;
        String fileDir = Util.getIDLFileDirectoryOnly(idlFilename);
        String javafile = (m_outputDir != null ? m_outputDir : "") +
                (!m_package.isEmpty() ? m_package.replace('.', File.separatorChar) + File.separator : "") +
                Util.getIDLFileNameOnly(idlFilename) + "PubSub.java";
        String headerfile = m_outputDir + Util.getIDLFileNameOnly(idlFilename) + "PubSubJNI.h";
        int exitVal = -1;
        String javac = null;
        String javah = null;

        // First call javac
        if (m_os.contains("Windows"))
        {
            javac = "javac.exe";
        }
        else if (m_os.contains("Linux") || m_os.contains("Mac"))
        {
            javac = "javac";
        }

        // Add command
        lineCommand.add(javac);
        if (m_tempDir != null)
        {
            lineCommand.add("-d");
            lineCommand.add(m_tempDir);
        }

        if ( fileDir != null && !fileDir.isEmpty())
        {
            lineCommand.add("-sourcepath");
            lineCommand.add(m_outputDir);
        }

        lineCommand.add(javafile);

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, null, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch (Exception ex)
        {
            System.out.println(ColorMessage.error(
                        METHOD_NAME) + "Cannot execute the javac application. Reason: " + ex.getMessage());
            return false;
        }

        if (exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "javac application return an error " + exitVal);
            return false;
        }

        lineCommand = new ArrayList<String>();

        if (m_os.contains("Windows"))
        {
            javah = "javah.exe";
        }
        else if (m_os.contains("Linux") || m_os.contains("Mac"))
        {
            javah = "javah";
        }

        // Add command
        lineCommand.add(javah);
        lineCommand.add("-jni");
        if (m_tempDir != null)
        {
            lineCommand.add("-cp");
            lineCommand.add(m_tempDir);
        }
        lineCommand.add("-o");
        lineCommand.add(headerfile);
        lineCommand.add((!m_package.isEmpty() ? m_package + "." : "") +
                Util.getIDLFileNameOnly(idlFilename) + "PubSub");

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, null, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch (Exception ex)
        {
            System.out.println(ColorMessage.error(
                        METHOD_NAME) + "Cannot execute the javah application. Reason: " + ex.getMessage());
            return false;
        }

        if (exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "javah application return an error " + exitVal);
            return false;
        }

        return true;
    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main entry point
     */

    public static void main(
            String[] args)
    {
        ColorMessage.load();

        if (loadPlatforms())
        {

            try
            {

                fastddsgen main = new fastddsgen(args);
                if (main.execute())
                {
                    System.exit(0);
                }

            }
            catch (BadArgumentException e)
            {

                System.out.println(ColorMessage.error("BadArgumentException") + e.getMessage());
                printHelp();

            }

        }

        System.exit(-1);
    }

}

class ProcessOutput extends Thread
{
    InputStream is = null;
    OutputStream of = null;
    String type;
    boolean m_check_failures;
    boolean m_found_error = false;
    final String clLine = "#line";
    boolean m_printLine = false;

    ProcessOutput(
            InputStream is,
            String type,
            boolean check_failures,
            OutputStream of,
            boolean printLine)
    {
        this.is = is;
        this.type = type;
        m_check_failures = check_failures;
        this.of = of;
        m_printLine = printLine;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (of == null)
                {
                    if (m_printLine)
                    {
                        System.out.println(line);
                    }
                }
                else
                {
                    // Sustituir los "\\" que pone cl.exe por "\"
                    if (line.startsWith(clLine))
                    {
                        line = "#" + line.substring(clLine.length());
                        int count = 0;
                        while ((count = line.indexOf("\\\\")) != -1)
                        {
                            line = line.substring(0, count) + "\\" + line.substring(count + 2);
                        }
                    }

                    of.write(line.getBytes());
                    of.write('\n');
                }

                if (m_check_failures)
                {
                    if (line.startsWith("Done (failures)"))
                    {
                        m_found_error = true;
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    boolean getFoundError()
    {
        return m_found_error;
    }

}
