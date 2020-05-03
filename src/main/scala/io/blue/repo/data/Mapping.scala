package io.blue.repo.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Mapping {

  @JsonProperty(value="name")
  var name: String  = _

  @JsonProperty(value="source_connection")
  var sourceConnection: String = _

  @JsonProperty(value="target_connection")
  var targetConnection: String = _

  @JsonProperty(value="source_table")
  var sourceTable: String = _

  @JsonProperty(value="target_table")
  var targetTable: String = _

  @JsonProperty(value="source_hint")
  var sourceHint: String = _

  @JsonProperty(value="target_hint")
  var targetHint: String = _

  @JsonProperty(value="filter")
  var filter: String = _

  @JsonProperty(value="batch_size")
  var batchSize: Int = 1000

  @JsonProperty(value="drop_create")
  var dropCreate: Boolean = false

  @JsonProperty(value="source_columns")
  var sourceColumns: java.util.List[String] = _

  @JsonProperty(value="target_columns")
  var targetColumns: java.util.List[String] = _

}