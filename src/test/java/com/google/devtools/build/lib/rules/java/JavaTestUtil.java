// Copyright 2021 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.rules.java;

import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.CommandLineExpansionException;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.analysis.actions.TemplateExpansionAction;
import com.google.devtools.build.lib.testutil.Scratch;
import com.google.devtools.build.lib.testutil.TestConstants;
import com.google.devtools.build.lib.util.OS;
import java.util.Objects;
import net.starlark.java.eval.EvalException;

public class JavaTestUtil {

  private JavaTestUtil() {}

  /** Writes BUILD file for java toolchain to scratch. */
  static void writeBuildFileForJavaToolchain(Scratch scratch) throws Exception {
    scratch.file("java/com/google/test/turbine_canary_deploy.jar");
    scratch.file("java/com/google/test/turbine_graal");
    scratch.file("java/com/google/test/tzdata.jar");
    scratch.overwriteFile(
        "java/com/google/test/BUILD",
        "java_toolchain(name = 'toolchain',",
        "    source_version = '6',",
        "    target_version = '6',",
        "    bootclasspath = ['rt.jar'],",
        "    xlint = ['toto'],",
        "    javacopts =['-Xmaxerrs 500'],",
        "    compatible_javacopts = {",
        "        'appengine': ['-XDappengineCompatible'],",
        "        'android': ['-XDandroidCompatible'],",
        "    },",
        "    tools = [':javac_canary.jar'],",
        "    javabuilder = [':JavaBuilder_deploy.jar'],",
        "    jacocorunner = ':jacocorunner.jar',",
        "    header_compiler = [':turbine_canary_deploy.jar'],",
        "    header_compiler_direct = [':turbine_graal'],",
        "    singlejar = ['singlejar'],",
        "    ijar = ['ijar'],",
        "    genclass = ['GenClass_deploy.jar'],",
        "    timezone_data = 'tzdata.jar',",
        "    java_runtime = ':jvm-k8'",
        ")",
        "java_runtime(",
        "    name = 'jvm-k8',",
        "    srcs = [",
        "        'k8/a', ",
        "        'k8/b',",
        "    ], ",
        "    java_home = 'k8',",
        ")",
        "toolchain(",
        "    name = 'java_toolchain',",
        "    toolchain = ':toolchain',",
        "    toolchain_type = '" + TestConstants.TOOLS_REPOSITORY + "//tools/jdk:toolchain_type',",
        ")");
  }

  public static String getJvmFlagsForJavaBinaryExecutable(Action action)
      throws CommandLineExpansionException, InterruptedException, EvalException {
    if (OS.getCurrent() == OS.WINDOWS) {
      return ((SpawnAction) action)
          .getArguments().stream()
              .filter(a -> a.startsWith("jvm_flags="))
              .flatMap(a -> stream(a.substring("jvm_flags=".length()).split("\t")))
              .collect(joining(" "));
    } else {
      return ((TemplateExpansionAction) action)
          .getSubstitutions().stream()
              .filter(s -> Objects.equals(s.getKey(), "%jvm_flags%"))
              .collect(onlyElement())
              .getValue();
    }
  }
}
