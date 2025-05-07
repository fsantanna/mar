function mar_equal (x, y)
    if type(x) ~= type(y) then
        return false
    elseif type(x) == "table" then
        for k in pairs(x) do
            if not mar_equal(x[k], y[k]) then
                return false
            end
        end
        for k in pairs(y) do
            if not mar_equal(x[k], y[k]) then
                return false
            end
        end
        return true
    else
        return x == y
    end
end

function mar_tostring (v)
    if type(v) ~= "table" then
        return tostring(v)
    elseif v.__type == "tuple" then
        local vs = ""
        for i=1, #v do
            if i > 1 then
                vs = vs .. ","
            end
            vs = vs .. mar_tostring(v[i])
        end
        return "[" .. vs .. "]"
    else
        error("TODO")
    end
end

function dump (...)
    local ret = {}
    for i=1, select("#", ...) do
        ret[#ret+1] = mar_tostring(select(i, ...))
    end
    print(table.unpack(ret))
    return table.unpack(ret)
end

// === MAR_MAIN === //