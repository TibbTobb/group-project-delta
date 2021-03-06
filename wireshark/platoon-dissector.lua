print("Registering platoon dissector")


local PORT_NUMBER = 5187
local PACKET_LENGTH = 200

-- creates a Proto object, but doesn't register it yet
local platoon = Proto("platoon","Platooning Protocol")


-- Message types lookup
local EMERGENCY_TYPE = 0
local DATA_TYPE = 1
local RTM_TYPE = 2
local ATM_TYPE = 3
local CONFIRM_TYPE = 4
local COMPLETE_TYPE = 5
local BEACON_Q_TYPE = 6
local BEACON_A_TYPE = 7

local types = {
    [EMERGENCY_TYPE] = "Emergency",
    [DATA_TYPE] = "Data",
    [RTM_TYPE] = "Request To Merge",
    [ATM_TYPE] = "Accept To Merge",
    [CONFIRM_TYPE] = "Merge Confirmation",
    [COMPLETE_TYPE] = "Merge Commit",
    [BEACON_Q_TYPE] = "Beacon ID Question",
    [BEACON_A_TYPE] = "Beacon ID Answer"
}

-- create the fields
local pf_type_field = ProtoField.uint8("platoon.type", "Message Type", base.DEC, types)
local pf_platoon_field = ProtoField.uint32("platoon.platoon", "Destination Platoon")
local pf_vehicle_field = ProtoField.uint32("platoon.vehicle", "Vehicle ID")

-- Data fields
local pf_speed_field = ProtoField.double("platoon.data.speed", "Speed (m/s)")
local pf_accel_field = ProtoField.double("platoon.data.accel", "Acceleration (m/s/s)")
local pf_tr_field = ProtoField.double("platoon.data.tr", "Turn Rate (rad/s)")
local pf_chosen_speed_field = ProtoField.double("platoon.data.chosen_speed", "Chosen Speed (m/s)")
local pf_chosen_accel_field = ProtoField.double("platoon.data.chosen_accel", "Chosen Acceleration (m/s/s)")
local pf_chosen_tr_field = ProtoField.double("platoon.data.chosen_tr", "Chosen Turn Rate (rad/s)")


-- Merging fields
local pf_transaction_field = ProtoField.uint32("platoon.merging.transaction_id", "Merge Transaction ID")
local pf_length_platoon_field = ProtoField.uint24("platoon.merging.length_platoon", "Length of the platoon")
local pf_platoon_member_field = ProtoField.uint32("platoon.merging.platoon_member", "Platoon Member")

 -- RTM specific
local pf_merging_platoon_field = ProtoField.uint32("platoon.merging.rtm.merging_platoon", "Merging Platoon ID")

-- ATM specific
local pf_merge_accepted_field = ProtoField.bool("platoon.merging.atm.accepted", "Merge Accepted?", 16, {"Yes", "No"}, 0x0100)
local pf_length_rename_field = ProtoField.uint32("platoon.merging.atm.length_renames", "Number of renames")
local pf_name_initial_field = ProtoField.uint32("platoon.merging.atm.name_initial", "Initial name")
local pf_name_final_field = ProtoField.uint32("platoon.merging.atm.name_final", "Final name")

-- Beacon Q/A fields
local pf_beacon_id_field = ProtoField.uint32("platoon.beacon.id", "Beacon ID")
local pf_beacon_asking_field = ProtoField.uint32("platoon.beacon.ask_id", "The platoon being asked")
local pf_beacon_response_field = ProtoField.uint32("platoon.beacon.response_id", "The responding platoon")

-- Register the fields
platoon.fields = {pf_type_field, pf_platoon_field, pf_vehicle_field,
    pf_transaction_field, pf_merging_platoon_field, pf_length_platoon_field,
    pf_platoon_member_field, pf_merge_accepted_field, pf_beacon_id_field,
    pf_beacon_asking_field, pf_beacon_response_field, pf_length_rename_field,
    pf_name_initial_field, pf_name_final_field, pf_speed_field, pf_accel_field,
    pf_tr_field, pf_chosen_speed_field, pf_chosen_accel_field, pf_chosen_tr_field
}


-- Find the value of some of the fields, so they can be used in the dissector
local types_field = Field.new("platoon.type")
local platoon_field = Field.new("platoon.platoon")
local vehicle_field = Field.new("platoon.vehicle")

-- Actually assign the dissector
function platoon.dissector(tvbuf, pktinfo, root)

    -- Set the protocol column to show our protocol name
    pktinfo.cols.protocol:set("Platooning")
    local info_string = ""

    -- Find out the packet size
    local pktlen = tvbuf:reported_length_remaining()

    if pktlen ~= PACKET_LENGTH then
        return
    end

    -- Add the protocol to the protocol trees
    local tree = root:add(platoon, tvbuf:range(0,pktlen))

    -- Read the common fields
    tree:add(pf_type_field, tvbuf:range(0,1))
    local type = tvbuf:range(0,1):uint()

    tree:add(pf_platoon_field, tvbuf:range(4,4))

    tree:add(pf_vehicle_field, tvbuf:range(8,4))

    info_string = info_string .. types_field().display ..", Platoon: " .. platoon_field().display

    if type == RTM_TYPE or type == ATM_TYPE or
            type == CONFIRM_TYPE or type == COMPLETE_TYPE then
        local merge_tree = tree:add("Merging")
        merge_tree:add(pf_transaction_field, tvbuf:range(12, 4))
        info_string = info_string ..", Transaction: "..tvbuf:range(12, 4):uint()

        local pos = 16
        if type == RTM_TYPE then
            merge_tree:add(pf_merging_platoon_field, tvbuf:range(pos, 4))
            info_string = info_string ..", Merging Platoon: "..tvbuf:range(pos, 4):uint()
            pos = pos + 4
        elseif type == ATM_TYPE then
            merge_tree:add(pf_merge_accepted_field, tvbuf:range(pos, 1))
        end
        if type == RTM_TYPE or type == ATM_TYPE then
            local length_tree = merge_tree:add(pf_length_platoon_field, tvbuf:range(pos+1, 4))
            local length = tvbuf:range(pos+1,3):uint()
            pos = pos + 4
            for i = 0, length-1, 1 do
                length_tree:add(pf_platoon_member_field, tvbuf(pos, 4))
                pos = pos + 4
            end
            local rename_tree = merge_tree:add(pf_length_rename_field, tvbuf:range(pos, 4))
            local rename_length = tvbuf:range(pos, 4):uint()
            for i = 0, rename_length-1, 1 do
                rename_tree:add(pf_name_initial_field, tvbuf(pos, 4))
                pos = pos + 4
                rename_tree:add(pf_name_final_field, tvbuf(pos, 4))
                pos = pos + 4
            end
        end
    elseif type == BEACON_A_TYPE or type == BEACON_Q_TYPE then
        if type == BEACON_Q_TYPE then
            tree:add(pf_beacon_asking_field, tvbuf:range(12,4))
        else
            tree:add(pf_beacon_response_field, tvbuf:range(12,4))
        end
        tree:add(pf_beacon_id_field, tvbuf:range(16,4))
        info_string = info_string .. ", Beacon ID: "..tvbuf:range(16, 4):uint()
    elseif type == DATA_TYPE then
        local data_tree = tree:add("Data")

        data_tree:add(pf_speed_field, tvbuf:range(12,8))
        data_tree:add(pf_accel_field, tvbuf:range(20,8))
        data_tree:add(pf_tr_field, tvbuf:range(28,8))
        data_tree:add(pf_chosen_speed_field, tvbuf:range(36,8))
        data_tree:add(pf_chosen_accel_field, tvbuf:range(44,8))
        data_tree:add(pf_chosen_tr_field, tvbuf:range(52,8))
    end

    -- Set the info field of the packet
    pktinfo.cols.info:set(info_string)
    return pktlen
end

-- Listen to UDP traffic on the port specified by PORT_NUMBER
DissectorTable.get("udp.port"):add(PORT_NUMBER, platoon)

-- Finished
