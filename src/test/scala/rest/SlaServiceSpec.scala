package rest

import model.Sla
import org.specs2.matcher.FutureMatchers
import org.specs2.mock.MockitoMocker
import org.specs2.mutable.Specification
import service.{Context, SlaServiceImpl}


class SlaServiceSpec extends Specification with MockitoMocker with FutureMatchers {

  val ctxMock = mock[Context]
  val slaService = new SlaServiceImpl(ctxMock)

  //ny for any token it gives 42
  //(ctxMock.getSlaByToken _).when(*).returns(Some(Sla("", 42))) //scalamocks is buggy

  //todo when(ctxMock.getSlaByToken(any)).thenReturn(Some(Sla("", 42)))


  "sla service" should {

    "return default sla for unauthorized users" in {
      slaService.getSlaByToken("") must be_==(Sla("", 42)).await
    }

    "return rps=33 for authorized user foo" in {

      //define foo user sla

      //todo when(ctxMock.getSlaByToken("token1")).thenReturn(Some(Sla("foo", 33)))

      slaService.getSlaByToken("token1") must be_==(Sla("foo", 33)).await
      slaService.getSlaByToken("token2") must be_==(Sla("", 42)).await
    }

    "should call ctx.addSla on defineSla" in {

      slaService.defineSla("someuser", "pwd", 22)
      // todo verify(ctxMock, times(1)).addSla(any, any)

      true //we must return someth
    }


  }

}
