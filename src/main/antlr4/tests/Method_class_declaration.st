Object subclass: #SampleClass.

!SampleClass class methodsFor: 'group-a'!

classMethod1
  ^ True!

classMethod1: arg1
  ^ arg1 !

classMethod1: arg1 withArg: arg2
  ^ arg2
! !

!SampleClass class methodsFor: 'group-b'!

classMethod2
  ^ True !

classMethod2: arg1
  ^ arg1
!

classMethod2: arg1 withArg: arg2
  ^ arg2 !!

