package org.janusgraph.graphdb.transaction;

import com.google.common.base.Preconditions;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.JanusGraphRelation;
import org.janusgraph.diskstorage.Entry;
import org.janusgraph.graphdb.database.EdgeSerializer;
import org.janusgraph.graphdb.internal.InternalRelation;
import org.janusgraph.graphdb.internal.InternalRelationType;
import org.janusgraph.graphdb.internal.InternalVertex;
import org.janusgraph.graphdb.relations.CacheEdge;
import org.janusgraph.graphdb.relations.CacheVertexProperty;
import org.janusgraph.graphdb.relations.RelationCache;
import org.janusgraph.graphdb.types.TypeInspector;
import org.janusgraph.graphdb.types.TypeUtil;
import org.apache.tinkerpop.gremlin.structure.Direction;

import java.util.Iterator;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class RelationConstructor {

    public static RelationCache readRelationCache(Entry data, StandardJanusGraphTx tx) {
        return tx.getEdgeSerializer().readRelation(data, false, tx);
    }

    public static Iterable<JanusGraphRelation> readRelation(final InternalVertex vertex, final Iterable<Entry> data, final StandardJanusGraphTx tx) {
        return new Iterable<JanusGraphRelation>() {
            @Override
            public Iterator<JanusGraphRelation> iterator() {
                return new Iterator<JanusGraphRelation>() {

                    Iterator<Entry> iter = data.iterator();
                    JanusGraphRelation current = null;

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public JanusGraphRelation next() {
                        current = readRelation(vertex,iter.next(),tx);
                        return current;
                    }

                    @Override
                    public void remove() {
                        Preconditions.checkState(current!=null);
                        current.remove();
                    }
                };
            }
        };
    }

    public static InternalRelation readRelation(final InternalVertex vertex, final Entry data, final StandardJanusGraphTx tx) {
        RelationCache relation = tx.getEdgeSerializer().readRelation(data, true, tx);
        return readRelation(vertex,relation,data,tx,tx);
    }

    public static InternalRelation readRelation(final InternalVertex vertex, final Entry data,
                                                final EdgeSerializer serializer, final TypeInspector types,
                                                final VertexFactory vertexFac) {
        RelationCache relation = serializer.readRelation(data, true, types);
        return readRelation(vertex,relation,data,types,vertexFac);
    }


    private static InternalRelation readRelation(final InternalVertex vertex, final RelationCache relation,
                                         final Entry data, final TypeInspector types, final VertexFactory vertexFac) {
        InternalRelationType type = TypeUtil.getBaseType((InternalRelationType) types.getExistingRelationType(relation.typeId));

        if (type.isPropertyKey()) {
            assert relation.direction == Direction.OUT;
            return new CacheVertexProperty(relation.relationId, (PropertyKey) type, vertex, relation.getValue(), data);
        }

        if (type.isEdgeLabel()) {
            InternalVertex otherVertex = vertexFac.getInternalVertex(relation.getOtherVertexId());
            switch (relation.direction) {
                case IN:
                    return new CacheEdge(relation.relationId, (EdgeLabel) type, otherVertex, vertex, data);

                case OUT:
                    return new CacheEdge(relation.relationId, (EdgeLabel) type, vertex, otherVertex, data);

                default:
                    throw new AssertionError();
            }
        }

        throw new AssertionError();
    }

}