package com.sksamuel.elastic4s.http.search

import com.sksamuel.elastic4s.http.search.queries.{QueryBuilderFn, SortContentBuilder}
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.suggestion.TermSuggestionDefinition
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}

import scala.collection.JavaConverters._

object SearchBodyBuilderFn {

  def apply(request: SearchDefinition): XContentBuilder = {

    val builder = XContentFactory.jsonBuilder()
    builder.startObject()

    request.query.map(QueryBuilderFn.apply).foreach(x => builder.rawField("query", new BytesArray(x.string)))
    request.postFilter.map(QueryBuilderFn.apply).foreach(x => builder.rawField("post_filter", new BytesArray(x.string)))

    if (request.explain.contains(true)) {
      builder.field("explain", true)
    }

    request.minScore.foreach(builder.field("min_score", _))
    if (request.searchAfter.nonEmpty) {
      builder.field("search_after", request.searchAfter.asJava)
    }

    if (request.sorts.nonEmpty) {
      builder.startArray("sort")
      request.sorts.foreach { sort =>
        builder.rawValue(new BytesArray(SortContentBuilder(sort).string))
      }
      builder.endArray()
    }

    if (request.suggs.nonEmpty) {
      builder.startObject("suggest")
      request.suggs.foreach {
        case term: TermSuggestionDefinition =>
          builder.startObject(term.name)
          term.text.foreach(builder.field("text", _))
          builder.startObject("term")
          builder.field("field", term.fieldname)
          term.analyzer.foreach(builder.field("analyzer", _))
          term.lowercaseTerms.foreach(builder.field("lowercase_terms", _))
          term.maxEdits.foreach(builder.field("max_edits", _))
          term.minWordLength.foreach(builder.field("min_word_length", _))
          term.maxInspections.foreach(builder.field("max_inspections", _))
          term.minDocFreq.foreach(builder.field("min_doc_freq", _))
          term.maxTermFreq.foreach(builder.field("max_term_freq", _))
          term.prefixLength.foreach(builder.field("prefix_length", _))
          term.size.foreach(builder.field("size", _))
          term.shardSize.foreach(builder.field("shard_size", _))
          term.sort.map(_.name().toLowerCase).foreach(builder.field("sort", _))
          term.stringDistance.map(_.name.toLowerCase).foreach(builder.field("string_distance", _))
          term.suggestMode.map(_.name().toLowerCase).foreach(builder.field("suggest_mode", _))
          builder.endObject()
          builder.endObject()
      }
      builder.endObject()
    }

    if (request.storedFields.nonEmpty) {
      builder.field("stored_fields", request.storedFields.asJava)
    }

    if (request.indexBoosts.nonEmpty) {
      builder.startArray("indices_boost")
      request.indexBoosts.foreach { case (name, double) =>
        builder.startObject()
        builder.field(name, double)
        builder.endObject()
      }
      builder.endArray()
    }

    // source filtering
    request.fetchContext foreach { context =>
      if (context.fetchSource) {
        if (context.includes.nonEmpty || context.excludes.nonEmpty) {
          builder.startObject("_source")
          builder.field("includes", context.includes.toList.asJava)
          builder.field("excludes", context.excludes.toList.asJava)
          builder.endObject()
        }
      } else {
        builder.field("_source", false)
      }
    }

    builder.endObject()
    builder
  }
}
