package blueeyes.concurrent

import Future._
import org.specs.Specification
import org.specs.util._

class FutureSpec extends Specification {
  "Future" should {
    "support cancel" in { 
      val f = Future.dead[String](new Exception("error"))

      f.error must eventually (beSomething)
    }
  }

  "Future.split" should {
    "split apart future of tuple" in {
      val (string, integer) = ("foo", 123).future.split

      string.value  must eventually (beEqualTo(Some("foo")))
      integer.value must eventually (beEqualTo(Some(123)))
    }
  }

  "Future implicit join" should {
    "glue together tuple of futures into single future of tuple" in {
      val (string, integer) = ("foo", 123).future.split

      (string, integer).join.value must eventually (beEqualTo(Some(("foo", 123))))
    }
  }
  
  "Future.deliver" should {
    "automatically canceled when delivering a lazy value that throws an error" in {
      val e = new Exception("foo")
      val f = new Future[String]()

      f.deliver(throw e)

      f.error must eventually (beEqualTo(Some(e)))
    }
    
    "deliver to a second listener even when the first one throws an error" in {
      val f = new Future[String]()
      var result: Option[String] = None
      
      f.deliverTo { s => 
        sys.error("misbehaving delivery handler")
      }.deliverTo { s => 
        result = Some(s)
      }
      
      f.deliver("foo")
      
      result must eventually (beEqualTo(Some("foo")))
    }
  }
  "Future.trap" should {
    "traps errors when listener is added before deliver" in {
      val f = new Future[String]()
      val e = new Exception("foo")
      var result: List[Throwable] = Nil

      f trap { errors => result = errors}

      f.deliverTo(v => throw e)

      f.deliver("foo")

      result must eventually (beEqualTo(List(e)))
    }
    "traps errors when listener is added after deliver" in {
      val f = new Future[String]()
      val e = new Exception("foo")
      var result: List[Throwable] = Nil

      f trap { errors => result = errors}

      f.deliver("foo")

      f.deliverTo(v => throw e)

      result must eventually (beEqualTo(List(e)))
    }
  }

  "Future.zip" should {
    "not be done until both futures are done" in {
      val f1 = new Future[Int]()
      val f2 = new Future[Int]()
      
      val z = f1.zip(f2)
      
      z.isDone must beEqualTo(false)
      
      f1.deliver(1)
      
      z.isDone must beEqualTo(false)
      
      f2.deliver(2)
      
      z.isDone must beEqualTo(true)
    }
    
    "deliver tuple of the result of each future" in {
      val f1 = new Future[Int]()
      val f2 = new Future[Int]()
      
      val z = f1.zip(f2)
      
      f1.deliver(1)
      f2.deliver(2)
      
      z.value must eventually (beSome((1, 2)))
    }
  }
  
  "Future.map" should {
    "propagate cancel" in {
      val f = Future.dead[String](new Exception("error"))
      
      f.map(s => s + s).error must eventually (beSomething)
    }
    
    "cancel mapped future when mapping function throws error" in {
      val e = new Exception("foo")
      
      val f = "foo".future
      
      f.map { string =>
        throw e
      }.error must eventually (beEqualTo(Some(e)))
    }
  }
  
  "Future.flatMap" should {
    "propagate cancel" in { 
      val f = Future.dead[String](new Exception("error"))
    
      f.flatMap { s =>
        "future should not deliver this handler".future
      }.error must eventually (beSomething)
    }
    
    "cancel mapped future when mapping function throws error" in {
      val e = new Exception("foo")
      
      val f = "foo".future
      
      f.flatMap { string =>
        throw e
      }.error must eventually (beEqualTo(Some(e)))
    }
  }
  
  "Future.filter" should {
    "cancel filtered future when filtering function throws error" in {
      val e = new Exception("foo")
      
      val f = "foo".future
      
      f.filter { string =>
        throw e
      }.error must eventually (beEqualTo(Some(e)))
    }
  }
  
  "Future.orElse" should {
    "try to cancel original future when returned future is canceled" in {
      val original = new Future[String]()
      
      original.orElse("foo").cancel
      
      original.isCanceled must eventually (beEqualTo(true))
    }
    
    "must return future that cannot be canceled directly" in {
      val original = new Future[String]()
      
      val returned = original.orElse("foo")
      
      returned.cancel
      
      returned.value must eventually (beEqualTo(Some("foo")))
    }
    
    "must return future that cannot be canceled indirectly" in {
      val original = new Future[String]()
      
      val returned = original.orElse("foo")
      
      original.cancel
      
      returned.value must eventually (beEqualTo(Some("foo")))
    }
    
    "must propagate successful delivery to returned future" in {
      val original = new Future[String]()
      
      val returned = original.orElse("foo")
      
      original.deliver("bar")
      
      returned.value must eventually (beEqualTo(Some("bar")))
    }
    
    "must cancel returned future only if factory for default throws exception" in {
      val original = new Future[String]()
      
      val returned = original.orElse { why =>
        sys.error("oh no")
      }
      
      original.cancel
      
      returned.isCanceled must eventually (beTrue)
    }
  }
}
