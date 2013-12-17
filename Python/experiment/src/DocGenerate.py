#!/usr/bin python
#coding=utf-8
############################
# @author Jason Wong
# @date 2013-12-08
############################
# connect to aotm db
# generate documents of songs
############################

import MySQLdb
import sys
import numpy
import pylab as pl
import logging
import os
from nltk.stem.lancaster import LancasterStemmer
import DBProcess
import matplotlib.pyplot as plt

# reload sys and set encoding to utf-8
reload(sys)
sys.setdefaultencoding('utf-8')
# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'log/docgenerate_log.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

# define some global varibale
DBHOST = 'localhost'
DBUSER = 'root'
DBPWD = 'wst'
DBPORT = 3306
DBNAME = 'aotm'
DBCHARSET = 'utf8'

#read stop words from stop words file
# return stop words list
def readStopwordsFromFile(filename):
  words = []

  stopfile = open(filename,"r")
  while 1:
    line = stopfile.readline()
    if not line:
      break
    line = line.rstrip('\n')
    line = line.strip()
    line = line.lower()
    if line not in words:
      words.append(line)
  stopfile.close()
  return words

#combine two stop words lists
#return a combined stopwords list/file
def combineTwoStopwordsFile():
  if os.path.exists("stopwords.txt"):
    print "stop words file is existing......"
    return
  first = readStopwordsFromFile("EnglishStopWords_datatang.txt")
  second = readStopwordsFromFile("EnglishStopWords_url.txt")
  result = list(set(first).union(set(second)))
  rFile = open("stopwords.txt","w")
  for word in result:
    rFile.write(word+'\n')
  rFile.close()
  return result

#get stopwords list
stopwords = readStopwordsFromFile("stopwords.txt")
vocabulary = []

#add word to word dictionary
#tagDict:word dictionary of a song(word:count)
#tagStr:tag string
#tagCount:the count of tag's appearance
def addItemToDict(tagDict,tagStr,tagCount):
  #include global variable
  global vocabulary
  #stemmer
  st = LancasterStemmer()
  #split tagStr
  items = tagStr.split()
  for item in items:
    item = item.lower()
    #stem
    item = st.stem(item)
    #remove stopwords and too short words
    if item not in stopwords and len(item) > 1:
      if item not in tagDict:
        tagDict[item] = tagCount
      else:
        tagDict[item] = tagDict[item] + tagCount
      #add item to vocabulary list
      if item not in vocabulary:
        vocabulary.append(item)

#generate tag dictionary of given song
def generateTagDictofSong(sname,aname,tags):
  tagDict = {}
  #add sname to tagDict
  addItemToDict(tagDict,sname,50)
  #add aname to tagDict
  addItemToDict(tagDict,aname,50)
  #split tags<tag:count>
  tagInfos = tags.split("##==##")
  #loop every tag Information
  for tagInfo in tagInfos:
    #cannot use split(":") because some tag contains ":" like <hello:world:3>
    #find : from right index
    index = tagInfo.rfind(":")
    tagStr = tagInfo[:index]
    tagCount = tagInfo[index+1:]
    #avoid some tags without count
    #if a tag has no count, ignore it
    if len(tagCount) == 0:
      continue
    else:
      #add tag to dict
      addItemToDict(tagDict,tagStr,int(tagCount))
  return tagDict

#rm dir,no matter it is empty or not
def rmDir(whichdir):
  print 'begin to rm dir %s' % whichdir
  for dirpath,dirname,filenames in os.walk(whichdir):
    for filename in filenames:
      filepath = os.path.join(dirpath,filename)
      os.remove(filepath)
  print 'delete %d files in %s' % (len(filenames),whichdir)
  os.rmdir(whichdir)
  print 'end of rming dir %s' % whichdir

#generate document of given song from its tagDict
def generateDocofSong(sid,tagDict):
  #if song file exists, return
  if os.path.exists("data/songs/%d" % sid):
    print '%d is existing...' % sid
    logging.warning('%d is existing...' % sid)
    return
  #else new a file
  sFile = open("data/songs/%d" % sid, "w")
  #repeat: write tag into file
  for tag in tagDict.keys():
    count = (int)(tagDict[tag] / 4)
    content = ""
    #construct a line and write to file
    for i in range(0,count):
      content = "%s %s" % (content,tag)
    sFile.write(content+'\n')
  sFile.close()

#generate all docs of all songs
#get statistics of tags,listener number and playcount
def generateDocs():
  #import global variable
  global DBHOST
  global DBUSER
  global DBPWD
  global DBPORT
  global DBNAME
  global DBCHARSET

  #rm folder songs
  rmDir("data/songs")
  #mkdir songs
  os.mkdir("data/songs")  

  try:
    #connect db and select db name
    conn = MySQLdb.Connect(host=DBHOST,user=DBUSER,passwd=DBPWD,port=DBPORT,charset=DBCHARSET)
    cur = conn.cursor()
    conn.select_db(DBNAME)
    #get song dict and playlist dict from DBProcess
    songDict,playlistDict = DBProcess.genEffectivePlaylist()
    #tags'count:number
    countDict = {}
    #listeners'count:number
    lisDict = {}
    #playcount:number
    playDict = {}
    songNum = len(songDict)
    index = 0
    for sid in songDict.keys():
      index = index + 1
      print 'begin to generate file of song %d(%d/%d)' % (sid,index,songNum)
      logging.debug('begin to generate file of song %d(%d/%d)' % (sid,index,songNum))
      #select info of a song with sid
      cur.execute('select sname,aname,count,tags,listeners,playcount,useful from effective_song where id = %d' % sid)
      result = cur.fetchone()
      sname = result[0]
      aname = result[1]
      count = int(result[2])
      #update count dict
      if count not in countDict:
        countDict[count] = 1
      else:
        countDict[count] = countDict[count] + 1
      tags = result[3]
      #update listener dict
      listeners = int(result[4])
      if listeners not in lisDict:
        lisDict[listeners] = 1
      else:
        lisDict[listeners] = lisDict[listeners] + 1
      #update playcount dict
      playcount = int(result[5])
      if playcount not in playDict:
        playDict[playcount] = 1
      else:
        playDict[playcount] = playDict[playcount] + 1

      useful = int(result[6])
      if useful == 0:
        print '%d useful is 0...' % sid
        logging.warning('%d useful is 0...' % sid)
        return

      tagDict = generateTagDictofSong(sname,aname,tags)
      generateDocofSong(sid,tagDict)
    
      print 'end of generating file of song %d' % sid
      logging.debug('end of generating file of song %d' % sid)
    conn.commit()
    cur.close()
    conn.close()
    print 'There are %d different tag count' % len(countDict)
    print 'There are %d different listener numbers' % len(lisDict)
    print 'There are %d different playcount numbers' % len(playDict)
    return countDict,lisDict,playDict
  except MySQLdb.Error,e:
    print 'Mysql Error %d:%s' % (e.args[0],e.args[1])
    logging.error('Mysql Error %d:%s' % (e.args[0],e.args[1]))

if __name__ == "__main__":
  generateDocs()
