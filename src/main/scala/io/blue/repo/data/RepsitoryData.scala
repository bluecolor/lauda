package io.blue.repo.data

import java.io.File
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

object RepositoryData {

  def read(file: String): RepositoryData = {
    val mapper = new ObjectMapper(new YAMLFactory)
    mapper.readValue(new File(file), classOf[RepositoryData])
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class RepositoryData {

  @JsonProperty(value="connections")
  var connections: java.util.List[Connection] = java.util.Collections.emptyList

  @JsonProperty(value="mappings")
  var mappings: java.util.List[Mapping] = java.util.Collections.emptyList

}
