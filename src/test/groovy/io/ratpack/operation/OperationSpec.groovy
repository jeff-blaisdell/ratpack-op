package io.ratpack.operation

import org.apache.commons.lang.exception.ExceptionUtils
import ratpack.exec.Blocking
import ratpack.exec.Operation
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class OperationSpec extends Specification {

    /**
     * Uncertain if relevant, but closest test found in RatPack core.
     *
     * Passes?
     * Test is green, but error differs then one thrown.
     * * Expected RuntimeException("Async Error!")
     * * Action "Promise.then() can only be called on a compute thread "
     * Tested in Ratpack Core
     * @see https://github.com/ratpack/ratpack/blob/master/ratpack-core/src/test/groovy/ratpack/exec/PromiseOperationsSpec.groovy#L341
     */
    void 'it should handle an operation failure when using blockingOp'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .blockingOp({
                    calls.asyncIndefinite()
                })
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }

    /**
     * Fails - Exception not handled.
     */
    void 'it should handle an operation failure when using operation'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .operation({
                    calls.asyncError()
                })
                .promise()
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }

    /**
     * Passes - Works as expected.
     * * Exception "RuntimeException("Async Error!")" is caught by .result
     */
    void 'it should handle an operation failure when using Operation.of'() {
        given:
        Calls calls = new Calls()

        when:
        ExecHarness.runSingle({ e ->
            println "start"
            calls.promiseVoid()
                .flatMap({
                    return calls.promiseString()
                })
                .flatMap({
                    return Operation.of({
                        calls.asyncError()
                    }).promise()
                })
                .result({ r ->
                    if (r.isError()) {
                        println ExceptionUtils.getStackTrace(r.getThrowable())
                        println 'total failure'
                    }
                    println 'end'
                })
        })

        then:
        noExceptionThrown()
    }

    class Calls {

        public void asyncError() {
            Blocking.get({
                throw new RuntimeException("Async Error!")
            }).then({
                println "oops"
            })
        }

        public void asyncIndefinite() {
            Blocking.get({
                while (true) {}
            }).then({
                println "not so indefinite eh?"
            })
        }

        public Promise<Void> promiseVoid() {
            return Promise.value(null)
        }

        public Promise<String> promiseString() {
            return Promise.value("Hello World!")
        }

    }
}
