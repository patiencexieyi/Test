package com.atguigu.mq.listener;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MyMessageListener {

    public static final String QUEUE_NAME  = "queue.order";
    public static final String QUEUE_NORMAL = "queue.normal.video";
    public static final String QUEUE_DEAD_LETTER = "queue.dead.letter.video";
    public static final String QUEUE_DELAY = "queue.test.delay";

    // @RabbitListener(queues = {QUEUE_NAME})
    public void processMessage(String dataString, Message message, Channel channel) throws IOException {

        // 获取当前消息的 deliveryTag
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 核心操作
            log.info("消费端 消息内容：" + dataString);

            System.out.println(10 / 0);

            // 核心操作成功：返回 ACK 信息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {

            // 获取当前消息是否是重复投递的
            //      redelivered 为 true：说明当前消息已经重复投递过一次了
            //      redelivered 为 false：说明当前消息是第一次投递
            Boolean redelivered = message.getMessageProperties().getRedelivered();

            // 核心操作失败：返回 NACK 信息
            // requeue 参数：控制消息是否重新放回队列
            //      取值为 true：重新放回队列，broker 会重新投递这个消息
            //      取值为 false：不重新放回队列，broker 会丢弃这个消息

            if (redelivered) {
                // 如果当前消息已经是重复投递的，说明此前已经重试过一次啦，所以 requeue 设置为 false，表示不重新放回队列
                channel.basicNack(deliveryTag, false, false);//multiple是否进行批量操作,requeue
            } else {
                // 如果当前消息是第一次投递，说明当前代码是第一次抛异常，尚未重试，所以 requeue 设置为 true，表示重新放回队列在投递一次
                channel.basicNack(deliveryTag, false, true);
            }

            // reject 表示拒绝
            // 辨析：basicNack() 和 basicReject() 方法区别
            // basicNack()能控制是否批量操作
            // basicReject()不能控制是否批量操作
            // channel.basicReject(deliveryTag, true);
        }
    }

    @RabbitListener(queues = {QUEUE_NAME})
    public void processMessageTestPrefetch(String dataString, Message message, Channel channel) throws IOException, InterruptedException {
        log.info("消费端 消息内容：" + dataString);

        TimeUnit.SECONDS.sleep(1);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(queues = {QUEUE_NORMAL})
    public void processMessageNormal(Message message, Channel channel) throws IOException {
        // 监听正常队列，但是拒绝消息
        log.info("★[normal]消息接收到，但我拒绝。");
        channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(queues = {QUEUE_DEAD_LETTER})
    public void processMessageDead(String dataString, Message message, Channel channel) throws IOException {
        // 监听死信队列
        log.info("★[dead letter]dataString = " + dataString);
        log.info("★[dead letter]我是死信监听方法，我接收到了死信消息");
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(queues = {QUEUE_DELAY})
    public void processMessageDelay(String dataString, Message message, Channel channel) throws IOException {
        log.info("[delay message][消息本身]" + dataString);
        log.info("[delay message][当前时间]" + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    public static final String QUEUE_PRIORITY = "queue.test.priority";

    @RabbitListener(queues = {QUEUE_PRIORITY})
    public void processMessagePriority(String dataString, Message message, Channel channel) throws IOException {
        log.info("[priority]" + dataString);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}