package service

/**
  * Created by taras.beletsky on 8/20/16.
  */
trait SlaRequirements {

  //  Rules for RPS counting:
  //  1. If no token provided, assume the client as unauthorized.
  val RuleNoToken = ()
  //    All unauthorized user's requests are limited by GraceRps
  val RuleUnauthGrace = ()
  //
  //  2. If request has a token, but slaService has not returned any info yet, treat it as unauthorized user
  val RuleTokenNoSla = ()
  //
  //  3. If request has token, and we have maxRPS for the user -- count RPS by its username (user may have several different tokens)
  val RuleTokenSla = ()
  //
  //  4. SLA should be counted by intervals of 1/10 second (i.e. if RPS limit is reached, after 1/10 second ThrottlingService should allow 10% more requests)
  val RuleRpsLimit = ()
  //
  //  Note:
  //  1. SLA information is changed quite rarely and SLAService is quite costly to call, so consider caching SLA requests.
  // Also, you should not query the service, if the s           ame token request is already in progress.
  val NoteCache = ()
  //
  //  2. ThrottlingService must be threadsafe, will be called from several threads concurrently.
  //    Must not wait or block, it should return the result immediately.
  //    Use as less locking and synchronization as possible
  //
  //  The solution should contain a project with
  //  * implementation of ThrottlingService
  //    * tests that prove validity of the code
  //    * load test to measure overhead of using ThrottlingService service, compared with same rest endpoint with no ThrottlingService

}
