Object subclass: #ClassVariableCompilerTest
       instanceVariableNames: ''
       classVariableNames: 'fieldA fieldB'.

!ClassVariableCompilerTest methodsFor: 'testing'!

tryReadClassField
    ^ fieldA!
!

!ClassVariableCompilerTest class methodsFor: 'access'!

fieldA
    ^ fieldA!

fieldA: value
    fieldA := value!

fieldB
    ^ fieldB!

fieldB: value
    fieldB := value!

fieldC
    ^ fieldC!
!

ClassVariableCompilerTest
