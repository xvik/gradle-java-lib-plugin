package sample

import spock.lang.Specification

class SampleTest2 extends Specification{

    def "Check sample"() {

        expect:
        new Sample2().foo() == 'test'
    }
}