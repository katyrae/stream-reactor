package com.datamountaineer.streamreactor.connect.cassandra.sink

import java.util.UUID

import com.datamountaineer.streamreactor.connect.cassandra.TestConfig
import com.datamountaineer.streamreactor.connect.cassandra.config.CassandraConfigConstants
import com.datastax.driver.core.Session
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.connect.sink.{SinkRecord, SinkTaskContext}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, Matchers, WordSpec}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}

import scala.collection.JavaConverters._

@DoNotDiscover
class TestCassandraJsonWriterUnset
    extends WordSpec
    with Matchers
    with MockitoSugar
    with TestConfig
    with BeforeAndAfterAll {

  val keyspace = "sink"
  val contactPoint = "localhost"
  val userName = "cassandra"
  val password = "cassandra"
  var session : Session = _

  override def beforeAll {
    session = createKeySpace(keyspace ,secure = true, ssl = false)
  }

  override def afterAll(): Unit = {
    session.close()
    session.getCluster.close()
  }

  "a Cassandra json unset feature" should {
    "pre-existing values overridden if using Struct schema" in {
      val table = "A" + UUID.randomUUID().toString.replace("-", "_")
      val context = mock[SinkTaskContext]
      when(context.assignment()).thenReturn(getAssignment)
      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.$table (
        id text PRIMARY KEY
        , col_a bigint
        , col_b bigint)""")

      val schema = SchemaBuilder.struct
        .field("id", Schema.STRING_SCHEMA)
        .field("col_a", Schema.INT64_SCHEMA)
        .field("col_b", Schema.OPTIONAL_INT64_SCHEMA)
        .build()

      val config = Map(
        CassandraConfigConstants.CONTACT_POINTS -> contactPoint,
        CassandraConfigConstants.KEY_SPACE -> keyspace,
        CassandraConfigConstants.USERNAME -> userName,
        CassandraConfigConstants.PASSWD -> password,
        CassandraConfigConstants.KCQL -> s"INSERT INTO $table SELECT * FROM TOPIC",
        CassandraConfigConstants.DEFAULT_VALUE_SERVE_STRATEGY_PROPERTY -> "UNSET"
      ).asJava

      val (init, part) = (
        new SinkRecord("TOPIC", 0, Schema.STRING_SCHEMA, "key", schema, new Struct(schema).put("id", "1").put("col_a", 4L).put("col_b", 5L),
          0, System.currentTimeMillis(), TimestampType.LOG_APPEND_TIME),
        new SinkRecord("TOPIC", 0, Schema.STRING_SCHEMA, "key", schema, new Struct(schema).put("id", "1").put("col_a", 6L),
          0, System.currentTimeMillis(), TimestampType.LOG_APPEND_TIME)
      )

      val task = new CassandraSinkTask()
      task.initialize(context)
      task.start(config)
      task.put(Seq(init).asJava)
      task.put(Seq(part).asJava)
      task.stop()

      val res = session.execute(s"SELECT * FROM $keyspace.$table")
      val row = res.one()
      System.out.println(row)
      row.getLong("col_a") shouldBe 6L
      row.getLong("col_b") shouldBe 0L //not equal to 5L

    }

    "pre-existing values will be preserved " in {
      val colA_expected = 3L
      val colB_expected = 5L

      def getRecords(table: String): (SinkRecord, SinkRecord) = {
        val schema = SchemaBuilder.string().build()

        val record        = s"""{"id":"1","col_a":4,"col_b":$colB_expected}"""
        val partialRecord = s"""{"id":"1","col_a":$colA_expected}"""  // absent col_b

        (
          new SinkRecord(table,
                         0,
                         Schema.STRING_SCHEMA,
                         "key",
                         schema,
                         record,
                         0,
                         System.currentTimeMillis(),
                         TimestampType.LOG_APPEND_TIME),
          new SinkRecord(table,
                         0,
                         Schema.STRING_SCHEMA,
                         "key",
                         schema,
                         partialRecord,
                         0,
                         System.currentTimeMillis(),
                         TimestampType.LOG_APPEND_TIME)
        )
      }

      val table = "A" + UUID.randomUUID().toString.replace("-", "_")
      val kcql = s"INSERT INTO $table SELECT * FROM TOPICA"

      session.execute(s"""CREATE TABLE IF NOT EXISTS $keyspace.$table (
        id text PRIMARY KEY
        , col_a bigint
        , col_b bigint)""")

      val context = mock[SinkTaskContext]
      val assignment = getAssignment
      when(context.assignment()).thenReturn(assignment)
      val testRecords = getRecords("TOPICA")
      val config = Map(
        CassandraConfigConstants.CONTACT_POINTS -> contactPoint,
        CassandraConfigConstants.KEY_SPACE -> keyspace,
        CassandraConfigConstants.USERNAME -> userName,
        CassandraConfigConstants.PASSWD -> password,
        CassandraConfigConstants.KCQL -> kcql,
        CassandraConfigConstants.DEFAULT_VALUE_SERVE_STRATEGY_PROPERTY -> "UNSET"
      ).asJava

      val task = new CassandraSinkTask()
      task.initialize(context)
      task.start(config)

      val (init, part) = testRecords
      println(part)

      task.put(Seq(init).asJava)
      task.put(Seq(part).asJava)
      task.stop()

      val res = session.execute(s"SELECT * FROM $keyspace.$table")
      val row = res.one()
      row.getLong("col_a") shouldBe colA_expected
      row.getLong("col_b") shouldBe colB_expected
    }

    "pre-existing values will be set to null (default behavior)" in {
      val colA_expected = 3L
      val colB_unexpected = 5L

      def getRecords(table: String): (SinkRecord, SinkRecord) = {
        val schema = SchemaBuilder.string().build()

        val record        = s"""{"id":"1","col_a":4,"col_b":$colB_unexpected}"""
        val partialRecord = s"""{"id":"1","col_a":$colA_expected}"""  // absent col_b

        (
          new SinkRecord(table,
            0,
            Schema.STRING_SCHEMA,
            "key",
            schema,
            record,
            0,
            System.currentTimeMillis(),
            TimestampType.LOG_APPEND_TIME),
          new SinkRecord(table,
            0,
            Schema.STRING_SCHEMA,
            "key",
            schema,
            partialRecord,
            0,
            System.currentTimeMillis(),
            TimestampType.LOG_APPEND_TIME)
        )
      }

      //val table = "A" + UUID.randomUUID().toString.replace("-", "_")
      val table = "fake"
      val kcql = s"INSERT INTO $table SELECT * FROM TOPICA"

      session.execute(s"""CREATE TABLE IF NOT EXISTS $keyspace.$table (
        id text PRIMARY KEY
        , col_a bigint
        , col_b bigint)""")

      val context = mock[SinkTaskContext]
      val assignment = getAssignment
      when(context.assignment()).thenReturn(assignment)
      val testRecords = getRecords("TOPICA")
      val config = Map(
        CassandraConfigConstants.CONTACT_POINTS -> contactPoint,
        CassandraConfigConstants.KEY_SPACE -> keyspace,
        CassandraConfigConstants.USERNAME -> userName,
        CassandraConfigConstants.PASSWD -> password,
        CassandraConfigConstants.KCQL -> kcql
      ).asJava

      val task = new CassandraSinkTask()
      task.initialize(context)
      task.start(config)

      val (init, part) = testRecords
      println(part)

      task.put(Seq(init).asJava)
      task.put(Seq(part).asJava)
      task.stop()

      val res = session.execute(s"SELECT * FROM $keyspace.$table")
      val row = res.one()
      row.getLong("col_a") shouldBe colA_expected
      row.getLong("col_b") shouldBe 0   // null value
    }
  }
}
