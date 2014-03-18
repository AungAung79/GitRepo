/**
 * MTSA_AnalyzeSpout.java
 * 版权所有(C) 2014 
 * 创建:wwssttt 2014-03-07 22:41:00
 * 描述:得到上游数据，进行时序分析
 */
package com.wst.sweetfm.bolt;

import java.util.HashMap;
import java.util.Map;

import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class MTSA_AnalyzeBolt extends BaseRichBolt{
	
	private OutputCollector collector;
	private int analyzeIndex;
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		// TODO Auto-generated method stub
		this.collector = collector;
		this.analyzeIndex = context.getThisTaskIndex();
	}

	@Override
	public void execute(Tuple input) {
		// TODO Auto-generated method stub
		String subSeq = input.getString(0);
		System.err.println("analyzeIndex = "+this.analyzeIndex+" subSeq = "+subSeq);
		String[] items = subSeq.split("#");
		Integer tid = Integer.valueOf(items[0]);
		String topicStr = items[1];
		String[] topicItems = topicStr.split(">");
		double[] topicProbability = new double[topicItems.length];
		for(int i = 0; i < topicItems.length; i++){
			topicProbability[i] = Double.valueOf(topicItems[i]);
		}
		try{
			//arima预测
			RConnection rc = new RConnection();
			rc.voidEval("library('forecast')");
			
			rc.assign("probability", topicProbability);
			rc.voidEval("xseries <- ts(probability)");
			rc.voidEval("fit<-auto.arima(xseries,trace=T)");
			RList rl = rc.eval("xfore<-forecast(fit,h=1,fan=T)").asList();
			
			double[] result = rl.at("mean").asDoubles();
			
			double nextProbability =  result[0];
			
			rc.close();
			
			String resultStr = tid+":"+nextProbability;
			
			System.err.println("analyzeIndex = "+this.analyzeIndex+" origin = "+subSeq+" result = "+resultStr);
			
			this.collector.emit(new Values(resultStr));
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		declarer.declare(new Fields("next"));
	}

}