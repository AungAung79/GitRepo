package com.wst.sweetfm.bolt;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import com.wst.sweetfm.util.MTSA_Const;
import com.wst.sweetfm.util.SongTopicReader;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class MTSA_DPRC_SplitBolt extends BaseBasicBolt{
	
	@Override
	public void execute(Tuple input, BasicOutputCollector collector) {
		// TODO Auto-generated method stub
		Object id = input.getValue(0);
		String originalSeq = input.getString(1);
		Queue<String> queue = splitOriginalSequence(originalSeq);
		while(!queue.isEmpty()){
			collector.emit(new Values(id,queue.poll()));
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		declarer.declare(new Fields("id", "probability_sequence"));
	}
	
	//split an original sequence like "0>1>2>3" to K dimensions(as the same as topic number)
	private Queue<String> splitOriginalSequence(String originalSeq){
		HashMap<Integer,HashMap<Integer,Double>> songMap = SongTopicReader.getSongMap();
		System.err.println("reader count = "+SongTopicReader.count);
		Queue<String> queue = new LinkedList<String>();
		String[] items = originalSeq.split(">");
		Integer[] sids = new Integer[items.length];
		for(int i = 0; i < sids.length; i++){
			sids[i] = Integer.valueOf(items[i]);
		}
		for(int tid = 0; tid < MTSA_Const.TOPIC_NUM; tid++){
			StringBuffer subSeq = new StringBuffer();
			subSeq.append(tid+"#");
			for(int sIndex = 0; sIndex < sids.length; sIndex++){
				Integer sid = sids[sIndex];
				if (sIndex == (sids.length - 1)){
					subSeq.append(songMap.get(sid).get(tid));
				}else{
					subSeq.append(songMap.get(sid).get(tid)+">");
				}
			}
			queue.add(subSeq.toString());
		}
		return queue;
	}
}