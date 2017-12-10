package test.es

import org.scalatest.FlatSpec
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.annotation.JsonInclude
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import scala.concurrent.Future
import scala.concurrent.Await
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Calendar

case class JsonRoot(aggregations:Aggregations)
case class Aggregations(agg01:Agg01)
case class Agg01(buckets:Seq[Bucket])
case class Bucket(key:String,doc_count:Int)


class AggregatorSpec extends FlatSpec{
  
  import test.es.Json._  
  
  /*
  
  it should "jsonRead" in{

    val objectMapper=new ObjectMapper().registerModule(DefaultScalaModule)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)    //.configure(SerializationFeature., state)
    
    val jsonRoot=objectMapper.readValue(json, classOf[JsonRoot])
    val buckets=jsonRoot.aggregations.agg01.buckets.sortBy(_.key)
    
    buckets.foreach{value=>
      println(value.key)
    }
    
  
    
  }
  */
  
  
  val objectMapper=new ObjectMapper().registerModule(DefaultScalaModule)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)    //.configure(SerializationFeature., state)

  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  val app = new GuiceApplicationBuilder().build()
  
  def request(url:String,query:Option[String]):Future[Either[(Int,String),String]]={
    
    println("**QUERY**")
    println(query)
    println("**QUERY**")
    
    val wsClient = app.injector.instanceOf(classOf[WSClient])
    
    
    query.fold{
      wsClient.url(url).get()
    }{query=>
      wsClient.url(url).post(query)
    }.map{response=>
      response.status match{
        case result:Int if 200 <= result  && result <= 299 =>
          Right(response.body)
        case result:Int=>  
          Left(result -> response.body)
      }
    }
  }
  
  
  def toDatetimeString(date:Date)=
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+09:00'").format(date)    
  
  
  it should "metrics一覧" in{
    
    val calendar = Calendar.getInstance
    val dateEnd = calendar.getTime
    calendar.add(Calendar.MINUTE, -15)
    val dateStart =calendar.getTime 
    
    val query = s"""
      |"query":{  
      |  "bool":{
      |    "must":[
      |      {
      |        "range":{"@timestamp":{
      |          "gte": "${toDatetimeString(dateStart)}",
      |          "lte": "${toDatetimeString(dateEnd)}"
      |        }}
      |      }
      |      ],
      |    "must_not":[],"should":[]
      |   }
      |},
      """.stripMargin
    
    val queryBody = s"""
            |{
            |  ${query} 
            |  "size": 0,
            |  "aggregations": {
            |    "agg01": {
            |      "terms": {
            |        "field": "canonical_metric.name.keyword"
            |        ,
            |        "size": 10000
            |      }
            |    }
            |  }
            |}      
            """.stripMargin
    
    
    
    val future = request("http://localhost:9200/applog-20171105/_search",Some(queryBody))
    
    Await.result(future, concurrent.duration.Duration.Inf) match{
      case Left((status,errorBody))=>
        println(s"[STATUS]${status}")
        println(s"[MSG]${errorBody}")
        
      case Right(json)=>  
        val jsonRoot=objectMapper.readValue(json, classOf[JsonRoot])
        val buckets=jsonRoot.aggregations.agg01.buckets.sortBy(_.key)
        println("[canonical_metric.name.keyword]")
        //kamon関連のmetircsは一覧から外す
        buckets.filter{bucket=> !bucket.key.contains(".kamon/")}.foreach{value=>
          //println(s"[canonical_metric.name.keyword]${index}:${value.key}")
          println(value.key)
        }
        println("[canonical_metric.name.keyword]")
    }   
    
    
    
    
    
    
  }
  
  it should "show fields 指定したインデックス、タイプのフィールド一覧を列挙" in {
    
    val IndexName = "applog-20171105"
    val Mappings = "mappings" 
    val TypeName = "app_log"
    
    val future = request(s"http://localhost:9200/${IndexName}/_${Mappings}",None)
    
    Await.result(future, concurrent.duration.Duration.Inf) match{
      case Left((status,errorBody))=>
        println(s"[STATUS]${status}")
        println(s"[MSG]${errorBody}")
        
      case Right(json)=>  
        val jsonRoot=objectMapper.readValue(json, classOf[Map[String,_]])
        
        def nestSearch(name:String,value:Any):Seq[(String,String)]={
          
          value match{
            case map:Map[String,_]=>
              
              val fieldList2=
              map.get("type").fold(Nil:Seq[(String,String)]){`type`=>
                Seq( name -> `type`.toString )
              }
              
              
              val fieldList3=
              map.get("properties").fold(fieldList2){_properties=>
                val properties  = _properties.asInstanceOf[Map[String,_]]
                
                
                fieldList2 ++
                properties.map{case(name2,value)=>
                  nestSearch(s"${name}.${name2}",value)
                }.flatten.toSeq
                
                //fieldList2 ++ y
              }
              
              
              //val fieldList4=
              map.get("fields").fold(fieldList3){_fields=>
                
                val fields  = _fields.asInstanceOf[Map[String,_]]
                fieldList3 ++ fields.map{case(name2,value)=>
                  nestSearch(s"${name}.${name2}",value)
                }.flatten.toSeq
                
              }
              //fieldList4
            case value:Any=>
              Nil
          }
        }
        
        
        val properties = jsonRoot(IndexName).asInstanceOf[Map[String,_]](Mappings).asInstanceOf[Map[String,_]](TypeName) //.asInstanceOf[Map[String,_]]("properties") //.asInstanceOf[Map[String,_]]
        
        val fieldList = nestSearch(TypeName,properties)
        
        
        fieldList.zipWithIndex.foreach{case((fullName,fieldType),index)=>
          
          println(s"[${index}]${fullName},${fieldType}")
          
        }
        
        println(s"${fieldList.length} <=> ${fieldList.map(_._1).length}")
        
        
        
        
    }   
    
    
    
    
    
    
  }
  
  
  
  
  
  
}