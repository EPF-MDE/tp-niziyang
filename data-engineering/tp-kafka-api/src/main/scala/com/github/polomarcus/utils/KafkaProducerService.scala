package com.github.polomarcus.utils

import com.github.polomarcus.conf.ConfService
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer._

import java.util.Properties

object KafkaProducerService {
  val logger = Logger(this.getClass)

  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfService.BOOTSTRAP_SERVERS_CONFIG)

  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "false") // For Question 3
  props.put(ProducerConfig.ACKS_CONFIG, "all")//We want to reduce the possibility of message loss, we should set acks=all to ensure that all synchronized replicas receive the message
  props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")//Enable idempotency to avoid message duplication due to retries.


  // @TODO this might be useful for compression (Question 2)
  // https://kafka.apache.org/documentation/#brokerconfigs_compression.type
  // props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, ???)
  // Enable compression to improve performance
  props.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
  // Batching settings for better throughput
  props.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
  props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024)); // 32 KB batch size

  private val producer = new KafkaProducer[String, String](props)

  def produce(topic: String, key: String, value: String): Unit = {
    val record = new ProducerRecord(topic, key, value)

    try {
      producer.send(record)

      logger.info(s"""
        Sending message with key "$key" and value "$value"
      """)
    } catch {
      case e:Exception => logger.error(e.toString)
    } finally { // --> "finally" happens everytime and the end, even if there is an error
      //@see on why using flush : https://github.com/confluentinc/confluent-kafka-python/issues/137#issuecomment-282427382
      //@TODO to speed up this function that send one message at the time, what could we do ? Batch processing of messages
      // Removed `flush()` to let Kafka handle sending in batches
      // Question 1
    }
  }

  def close() = {
    producer.flush()
    producer.close()
  }
}
