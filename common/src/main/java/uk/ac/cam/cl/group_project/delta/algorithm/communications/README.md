# Communications Packet Structure
All numbers are in 0-indexed bytes, stored in a big-endian format.

## Header:
Every packet contains a 12 byte header, optionally followed by a payload.

Bytes | Content
----- | -------
0 | The type of message being sent ([definition](#message-type-ids))
1-3 | The length of the message in bytes (including header). Only messages up to 200B are supported by the current implementation.
4-7 | The ID of the *destination* platoon
8-11 | The ID of the sending vehicle

### Message Type IDs
ID | Message Type
---| ------------
0 | Emergency stop ([definition](#emergency-stop))  
1 | Vehicle status message ([definition](#vehicle-status)) 
2 | Request to merge, sent by leader of the platoon that would like to merge with another platoon ([definition](#request-to-merge))
3 | Accept to merge, sent by leader of the platoon that is being merged into ([definition](#accept-to-merge))
4 | Confirm merge, sent by all members of both platoons ([definition](#confirm-merge))  
5 | Merge Complete, sent by leader of the platoon that initiated the merge ([definition](#merge-complete))
6 | Beacon ID query ([definition](#beacon-id-query))
7 | Beacon ID answer ([definition](#beacon-id-answer))


## Messages
### Emergency stop
Upon receipt of this message, every vehicle will emergency stop, regardless of platoon or position. This has no payload, although a reason could be added at a later time.

### Vehicle status
This is the normal data which is sent to each other vehicle to coordinate the algorithm. The payload of this message is defined in the `VehicleData` class.

### Request to merge
This is sent by a platoon (the "merging platoon") to initiate a merge with the platoon in front.

Defined in `RequestToMergeMessage`.

#### Payload:

Bytes | Content
------|--------
12-15 | Transaction ID, generated at random to avoid clashes
16-19 | The ID of the merging platoon
20 | Reserved (should be set to 0)
21-23 | The length of the merging platoon  
24+ | An ordered list of the ids of the members of the merging platoon

### Accept to merge
This is used to confirm by the leader of the front platoon that the merging platoon can go ahead with the merge.

Defined in `AcceptToMergeMessage`.

#### Payload:

Bytes | Content
------|--------
12-15 | Transaction id, same as in the request to merge
16 | `0` if the merge has been rejected, and `1` if it has been accepted. If it has been rejected, there is no further payload
17-19 | The length of the main platoon  
20-x | An ordered list of the ids of the members of the main platoon (as 4 byte integers)
x+1 - x+4 | The number of ids which need to be replaced to ensure that all vehicles have unique IDs
x+5 - end | A list of (old\_id, new\_id) telling vehicle `old_id` in the merging platoon its new id is `new_id`

### Confirm merge
This is sent by every member of both platoons to confirm they have the new information
and are ready to commit it.

Defined in `ConfirmMergeMessage`.

#### Payload:

Bytes | Content
------|--------
12-15 | Transaction id

### Merge Complete
This is sent by the leader of the merging platoon after it has seen that all of the 
members have confirmed the merge.

Defined in `MergeCompleteMessage`.

#### Payload:

Bytes | Content
------|--------
12-15 | Transaction id

### Beacon ID Query
When a vehicle can see a beacon, it will send a query to the platoons that it knows about to ask them whether they own that beacon ID, so that it can potentially initiate a merge.

Defined in `BeaconIdQuestion`.

#### Payload:

Bytes | Content
------|--------
12-15 | Sending platoon ID
16-19 | The beacon ID being queried

### Beacon ID Answer
If a vehicle owns that beacon, it will send an answer back to the platoon that queried it, identifying the platoon that it's in.

Defined in `BeaconIdAnswer`.

#### Payload

Bytes | Content
------|--------
12-15 | Owner's platoon ID
16-19 | The beacon ID that was queried
