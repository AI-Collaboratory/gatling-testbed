package bd.ciber.gatling

import scala.collection._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

class LookupCache {
  val cache  : concurrent.Map[String,String] = new ConcurrentHashMap() asScala
  val locked : concurrent.Map[String,Long]   = new ConcurrentHashMap() asScala

  // first thread to call lock(key) returns true, all subsequent ones will return false
  def lock ( key: String, who: Long ) : Boolean = locked.putIfAbsent(key, who ) == None

  // only the thread that first called lock(key) can call put, and then only once
  def put( key: String, value: String, who: Long ) =
    if ( locked.get( key ).get == who )
      if ( cache.get( key ) == None )
        cache.put( key, value )
      else
        throw new Exception( "You can't put more than one value into the cache! " + key )
    else
      throw new Exception( "You have not locked '" + key + "'" )
  
  // This is not thread safe. Expecting this call only before each test.
  def clear() =
    locked.clear()
    cache.clear()

  // any thread can call get - will block until a non-null value is stored in the cache
  // WARNING: if the thread that is holding the lock never puts a value, this thread will block forever
  def get( key: String ) = {
    if ( locked.get( key ) == None )
      throw new Exception( "Must lock '" + key + "' before you can get() it" )
    var result : Option[String] = None
    do {
      result = cache.get( key )
      if ( result == None ) Thread.sleep( 100 )
    } while ( result == None )
    result.get
  }
}
