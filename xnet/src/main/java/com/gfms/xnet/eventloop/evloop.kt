class EventLoop(var size: int) {
	// Threshold when the buffer should be optimised
	val OPT_THRESHOLD = 0.8

	// Used to execute non-timed events
	private var eventQueue = EventQueue(size)
	// Used for events that are deferred to be executed in the future
	private var eventBuffer = EventBuffer(size)

	// The event loop worker thread
	private val worker = thread { loop() }

	// Tries to submit the event
	fun sumbit(event: Event): Event {
		// not a timed event
		return if (event.time <= 0) {
			eventQueue.push(event)
		} else { eventBuffer.add(event) }
	}

	// The actual event loop
	private fun loop() {
		while (true) {
			processNext()
		}
	}

	// Find the next event to process and execute
	// and execute it
	private fun processNext(): Event {
		return if (!eventQueue.isEmpty()) {
			process(eventQueue.pop())
		} else { process(eventBuffer.findNext())}
 	}

 	// Process an event
 	private fun process(e: Event): Event {
 		if (e.type != EventType.EMPTY && e.type != EventType.ERROR) {
 			println(e.type.toString() + " : " + e.time + ":"+ String(e.data))
 			e.processed = true
 		}
 		return e
 	}
}

enum class EventType {
	EMPTY,
	ERROR,
	GET,
	SET,
	IO
}

open class Event(val time: Long, val type: EventType, val data: ByteArray, var processed: Boolean=false)

class EmptyEvent(): Event(-1, EventType.ERROR, msg.toByteArray(), true)

class ErrorEvent(msg: String): Event(currentTime(), EventType.ERROR, msg.toByteArray(), true)

class SimpleEvent(type: EventType, msg: String) : Event(-1, type, msg.toByteArray())

class TimedEvent(deferredMs: Long, type: EventType, data: ByteArray) : Event(currentTime() + deferredMs, type, data)


