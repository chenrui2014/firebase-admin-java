/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple executable for parsing the code coverage reported produced by Jacoco.
 */
public class CodeCoverageReporter {

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Invalid invocation of the CodeCoverageReporter.");
      System.out.println("Usage: CodeCoverageReporter <path>");
      return;
    }

    System.out.println();
    System.out.println("-------------------------------------------------------------------------");
    System.out.println(" Jacoco Coverage Report");
    System.out.println("-------------------------------------------------------------------------");

    boolean firstLine = true;
    Map<String, CoverageInfo> coverageInfo = new TreeMap<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (firstLine) {
          firstLine = false;
          continue;
        }

        CoverageInfo info = new CoverageInfo(line);
        String packageName = info.getPackage();
        CoverageInfo existing = coverageInfo.get(packageName);
        if (existing != null) {
          coverageInfo.put(packageName, existing.aggregate(info, packageName));
        } else {
          coverageInfo.put(packageName, info);
        }
      }

      int longestKey = findLongest(coverageInfo.keySet()) + 1;
      CoverageInfo overall = null;
      for (Map.Entry<String, CoverageInfo> entry : coverageInfo.entrySet()) {
        CoverageInfo info = entry.getValue();
        if (overall == null) {
          overall = info;
        } else {
          overall = overall.aggregate(info, "Overall");
        }
        System.out.println(info.getFormattedString(longestKey));
      }

      if (overall != null) {
        System.out.println();
        System.out.println(overall.getFormattedString(longestKey));
      }
      System.out.println();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static int findLongest(Collection<String> keys) {
    int longest = 0;
    for (String key : keys) {
      if (key.length() > longest) {
        longest = key.length();
      }
    }
    return longest;
  }

  private static class CoverageInfo {

    private final String pkg;
    private final long instructionsCovered;
    private final long totalInstructions;

    CoverageInfo(String pkg, long instructionsCovered, long totalInstructions) {
      this.pkg = pkg;
      this.instructionsCovered = instructionsCovered;
      this.totalInstructions = totalInstructions;
    }

    CoverageInfo(String line) {
      checkArgument(!Strings.isNullOrEmpty(line));
      String[] segments = line.split(",");
      this.pkg = segments[1];
      this.instructionsCovered = Long.parseLong(segments[4]);
      this.totalInstructions = Long.parseLong(segments[3]) + this.instructionsCovered;
    }

    String getPackage() {
      return pkg;
    }

    private double getCoverage() {
      if (totalInstructions == 0) {
        return 0;
      }
      return ((double) instructionsCovered / totalInstructions) * 100.0;
    }

    CoverageInfo aggregate(CoverageInfo other, String name) {
      return new CoverageInfo(
          name,
          this.instructionsCovered + other.instructionsCovered,
          this.totalInstructions + other.totalInstructions);
    }

    String getFormattedString(int titleLength) {
      String ratio = this.instructionsCovered + "/" + this.totalInstructions;
      return String.format("%1$-" + titleLength + "s %2$12s %3$8.2f%%", this.pkg,
          ratio, this.getCoverage());
    }
  }

}