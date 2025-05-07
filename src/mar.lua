function atm_tostring (v)
    if type(v) ~= "table" then
        return tostring(v)
    elseif v.__type == "tuple" then
        local vs = ""
        for i=1, #v do
            if i > 1 then
                vs = vs .. ","
            end
            vs = vs .. atm_tostring(v[i])
        end
        return "[" .. vs .. "]"
    else
        error("TODO")
    end
end

function dump (...)
    local ret = {}
    for i=1, select("#", ...) do
        ret[#ret+1] = atm_tostring(select(i, ...))
    end
    print(table.unpack(ret))
    return table.unpack(ret)
end

// === MAR_MAIN === //