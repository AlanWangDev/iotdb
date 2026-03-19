/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.queryengine.plan.relational.sql.ast;

import java.util.Locale;
import java.util.Optional;
import org.apache.iotdb.db.exception.sql.SemanticException;

import org.apache.iotdb.db.protocol.session.IClientSession.SqlDialect;
import org.apache.iotdb.db.queryengine.plan.relational.analyzer.Scope;
import org.apache.tsfile.utils.RamUsageEstimator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class Copy extends Statement {

  private final Query query;
  private final String filePath;
  private final List<Property> properties;
  private static final Set<String> SUPPORTED_PROPERTIES = new HashSet<>();
  private static final Set<String> SUPPORTED_FORMATS = new HashSet<>();

  static {
    SUPPORTED_PROPERTIES.add("format");
    SUPPORTED_PROPERTIES.add("header");
    SUPPORTED_PROPERTIES.add("table_name");
    SUPPORTED_PROPERTIES.add("tag_column_names");
    SUPPORTED_PROPERTIES.add("time_column_name");

    SUPPORTED_FORMATS.add("csv");
    SUPPORTED_FORMATS.add("tsfile");
  }

  private class CopyProperty {
    private String format;
    private boolean header;
    private String tableName;
    private List<String> tagColumnNames;
    private String timeColumnName;
    private boolean available;

    private CopyProperty() {
      format = null;
      header = false;
      tableName = null;
      tagColumnNames = null;
      timeColumnName = null;
      available = false;
    }

    /**
     * Validate and import the properties from the Copy statement.
     */
    private void importFromProperties() {
      // Validate properties
      if (properties == null) {
        throw new SemanticException("Copy statement's properties are not found. Properties are necessary for Copy statement.");
      }
      // Validate the keys of the properties
      String formatString = null;
      String headerString = null;
      String tableNameString = null;
      String tagColumnNamesString = null;
      String timeColumnNameString = null;

      final Set<String> propertyNames = new HashSet<>();
      for (final Property property : properties) {
        final String key = property.getName().getValue().toLowerCase(Locale.ENGLISH);
        if (!SUPPORTED_PROPERTIES.contains(key)) {
          throw new SemanticException("Copy statement property " + key + " is currently not allowed.");
        }
        if (!propertyNames.add(key)) {
          throw new SemanticException(String.format("Copy statement: Duplicate property: " + key));
        }
        if (!property.isSetToDefault()) {
          final String value = property.getNonDefaultValue().toString().toLowerCase(Locale.ENGLISH);
          switch (key) {
            case "format": {
              formatString = value;
              break;
            }
            case "header":  {
              headerString = value;
              break;
            }
            case "table_name": {
              tableNameString = value;
              break;
            }
            case "tag_column_names": {
              tagColumnNamesString = value;
              break;
            }
            case "time_column_name": {
              timeColumnNameString = value;
              break;
            }
          }
        }
      }
      for (final Property property : properties) {
        process(property, scope);
      }

      // Validate the values of the properties
      // Validate formatString
      if (formatString == null) {
        throw new SemanticException("Copy statement's properties: FORMAT value is necessary.");
      }
      if (!SUPPORTED_FORMATS.contains(formatString)) {
        throw new SemanticException(
            "Copy statement: FORMAT value '" + formatString + "' is currently not allowed. " +
                "Supported formats are: " + String.join(", ", SUPPORTED_FORMATS)
        );
      }
      format = formatString;

      // Validate headerString
      if (format.equals("csv")) {
        if (headerString == null) {
          throw new SemanticException("Copy statement's properties: HEADER value is necessary.");
        }
        if (!headerString.equals("true") && !headerString.equals("false")) {
          throw new SemanticException("Copy statement's properties: HEADER value must be true or false.");
        }
        header = headerString.equals("true");
      }

      // Validate tableNameString
      // This Copy class is only used in the table model, so no need to verify if it's a table model.
      if (format.equals("tsfile")) {
        if (tableNameString == null) {
          throw new SemanticException("Copy statement's properties: TABLE_NAME value is necessary.");
        }
        tableName = tableNameString;
      }

      // Validate tagColumnNamesString
      tagColumnNames = Arrays.asList(tagColumnNamesString.split(","));
      // INSERT_YOUR_CODE
      if (tagColumnNames != null) {
        for (int i = 0; i < tagColumnNames.length; i++) {
          if (tagColumnNames.get(i) != null) {
            tagColumnNames.set(i, tagColumnNames.get(i).trim());
          }
        }
      }
      
      // Validate timeColumnNameString
      timeColumnName = timeColumnNameString;
      available = true;
    }
  }
  CopyProperty copyProperty;

  public Copy(
      NodeLocation location, Query query, String filePath, List<Property> properties) {
    super(location);
    this.query = requireNonNull(query, "query is null");;
    this.filePath = requireNonNull(filePath, "filePath is null");;
    this.properties = requireNonNull(properties, "properties is null");;
    this.copyProperty = new CopyProperty();
  }

  public Query getQuery() {
    return query;
  }

  public String getFilePath() {
    return filePath;
  }

  public List<Property> getProperties() {
    return properties;
  }

  @Override
  public List<? extends Node> getChildren() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public int hashCode() {
    return Objects.hash(query, filePath, properties);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Copy copy = (Copy) o;
    return Objects.equals(query, copy.query)
        && Objects.equals(filePath, copy.filePath)
        && Objects.equals(properties, copy.properties);
  }

  @Override
  public String toString() {
    return "Copy{"
        + "query="
        + query
        + ", filePath='"
        + filePath
        + '\''
        + ", properties="
        + properties
        + '}';
  }

  private static final long INSTANCE_SIZE = RamUsageEstimator.shallowSizeOfInstance(Copy.class);

  @Override
  public long ramBytesUsed() {
    return INSTANCE_SIZE
        + query.ramBytesUsed()
        + RamUsageEstimator.sizeOf(filePath)
        + AstMemoryEstimationHelper.getEstimatedSizeOfNodeList(properties);
  }

  @Override
  public <R, C> R accept(AstVisitor<R, C> visitor, C context) {
    return visitor.visitCopyStatement(this, context);
  }

  /** the Properties methods **/
  public boolean arePropertiesAvailable() {
    return copyProperty.available;
  }

  public String getPropertyFormat() {
    return copyProperty.format;
  }

  public boolean getPropertyHeader() {
    return copyProperty.header;
  }

  public String getPropertyNameTable() {
    return copyProperty.tableName;
  }

  public List<String> getPropertyTagColumnNames() {
    return copyProperty.tagColumnNames;
  }

  public String getPropertyTimeColumnName() {
    return copyProperty.timeColumnName;
  }

  // called in 
  public void analyzeProperties() {
    copyProperty.importFromProperties();
  }
  /** the Properties methods **/

  public Scope analyze(Optional<Scope> context) {
    Scope queryScope = visitQuery(query, Optional.of(context));
    analyzeProperties();
    // no analyze for filePath (String) here 
    return queryScope;
  }
}
